package com.moa2.api.reservation.service.v2;

import com.moa2.api.reservation.dto.ReservationRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka 예매 이벤트 발행자
 * - 예매 요청 정보를 reservation-request 토픽에 비동기 발행
 * - 실패 시 호출자(ServiceV2)에서 보상 트랜잭션 처리
 */
@Slf4j
@Profile("v2")
@Component
@RequiredArgsConstructor
public class ReservationKafkaProducer {

    private static final String TOPIC = "reservation-request";

    private final KafkaTemplate<String, ReservationRequestEvent> kafkaTemplate;

    public void send(ReservationRequestEvent event) {
        log.info("Kafka 이벤트 발행 - eventId: {}, userId: {}, scheduleId: {}",
                event.getEventId(), event.getUserId(), event.getScheduleId());
        kafkaTemplate.send(TOPIC, String.valueOf(event.getUserId()), event);
    }
}
