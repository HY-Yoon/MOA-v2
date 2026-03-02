package com.moa2.api.reservation.service.v2;

import com.moa2.api.reservation.dto.ReservationRequestEvent;
import com.moa2.api.show.domain.entity.ScheduleSeat;
import com.moa2.api.show.domain.repository.ScheduleSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moa2.api.reservation.dto.ReservationDtoV2;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Kafka 예매 이벤트 Consumer
 * - 멱등성 체크 (Redis + DB Unique Key 이중 방어)
 * - 수동 커밋 (처리 성공 시에만 오프셋 커밋)
 * - DLQ: 3회 재시도 후 reservation-request.DLT 토픽으로 이동 (KafkaConfig 설정)
 */
@Slf4j
@Profile("v2")
@Component
@RequiredArgsConstructor
public class ReservationKafkaConsumer {

    private final ReservationPersistService persistService;
    private final ScheduleSeatRepository scheduleSeatRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String DEDUP_KEY_PREFIX = "kafka:dedup:";
    private static final String SEAT_STATUS_PREFIX = "seat:status:";

    @KafkaListener(topics = "reservation-request", groupId = "reservation-group")
    public void consume(ReservationRequestEvent event, Acknowledgment ack) {
        String eventId = event.getEventId();
        log.info("Kafka 이벤트 수신 - eventId: {}, userId: {}", eventId, event.getUserId());

        // 1. 멱등성 체크 (이미 처리된 이벤트면 스킵)
        String dedupKey = DEDUP_KEY_PREFIX + eventId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(dedupKey))) {
            log.warn("중복 이벤트 무시 - eventId: {}", eventId);
            ack.acknowledge(); // 오프셋은 커밋하여 다시 안 읽도록
            return;
        }

        try {
            // 2. 좌석 엔티티 조회 (Consumer 시점의 관리 상태)
            List<ScheduleSeat> seats = event.getScheduleSeatIds().stream()
                    .map(id -> scheduleSeatRepository.findById(id)
                            .orElseThrow(() -> new IllegalArgumentException("좌석을 찾을 수 없습니다: " + id)))
                    .toList();

            // 3. DB 저장 (기존 PersistService 재활용)
            ReservationDtoV2.ReserveResponse reserveResponse = persistService.saveReservationData(
                    event.getUserId(),
                    event.getScheduleId(),
                    seats,
                    event.getTotalAmount(),
                    event.getLockTtlMinutes()
            );

            // 4. 멱등성 키 저장 (TTL 24시간 — DLQ 재처리 충분히 커버)
            redisTemplate.opsForValue().set(dedupKey, "DONE", 24, TimeUnit.HOURS);

            // 4.5. 처리 결과 객체 JSON 저장 (폴링 응답용)
            try {
                String resultKey = "reservation:result:" + eventId;
                String resultJson = objectMapper.writeValueAsString(reserveResponse);
                redisTemplate.opsForValue().set(resultKey, resultJson, 30, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.error("예매 결과 JSON 직렬화 실패 - eventId: {}", eventId, e);
            }

            // 5. 예매 상태 업데이트 (폴링용)
            String statusKey = "reservation:status:" + eventId;
            redisTemplate.opsForValue().set(statusKey, "COMPLETED", 30, TimeUnit.MINUTES);

            // 6. 좌석 상태 확정
            for (Long seatId : event.getScheduleSeatIds()) {
                redisTemplate.opsForValue().set(SEAT_STATUS_PREFIX + seatId, "CONFIRMED", 30, TimeUnit.MINUTES);
            }

            log.info("Kafka 이벤트 처리 완료 - eventId: {}", eventId);

            // 7. 수동 커밋 (성공 시에만!)
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Kafka 이벤트 처리 실패 - eventId: {} | error: {}", eventId, e.getMessage(), e);

            // 실패 시 좌석 상태를 FAILED로 업데이트
            String statusKey = "reservation:status:" + eventId;
            redisTemplate.opsForValue().set(statusKey, "FAILED", 30, TimeUnit.MINUTES);

            // 좌석 점유 해제 (반납)
            for (Long seatId : event.getScheduleSeatIds()) {
                redisTemplate.delete(SEAT_STATUS_PREFIX + seatId);
            }

            // 예외를 다시 던져서 DLQ ErrorHandler가 처리하도록 위임
            throw e;
        }
    }
}
