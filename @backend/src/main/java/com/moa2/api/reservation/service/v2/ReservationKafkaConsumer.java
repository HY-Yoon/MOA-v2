package com.moa2.api.reservation.service.v2;

import com.moa2.api.reservation.dto.ReservationRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka 결제 완료 알림 Consumer
 * - 결제 완료 후 이메일/SMS 알림 발송 전용
 * - DB 저장 로직 없음 (결제 완료 시 이미 DB 반영됨)
 */
@Slf4j
@Profile("v2")
@Component
@RequiredArgsConstructor
public class ReservationKafkaConsumer {

    @KafkaListener(topics = "payment-notification", groupId = "notification-group")
    public void consume(ReservationRequestEvent event, Acknowledgment ack) {
        String eventId = event.getEventId();
        log.info("결제 완료 알림 이벤트 수신 - eventId: {}, userId: {}", eventId, event.getUserId());

        try {
            // TODO: 이메일 발송 서비스 연동
            // emailService.sendReservationConfirmation(event);

            // TODO: SMS 발송 서비스 연동
            // smsService.sendReservationNotification(event);

            log.info("결제 완료 알림 처리 완료 - eventId: {}", eventId);

            // 수동 커밋 (성공 시에만)
            ack.acknowledge();

        } catch (Exception e) {
            log.error("결제 완료 알림 처리 실패 - eventId: {} | error: {}", eventId, e.getMessage(), e);
            // 알림 실패는 비즈니스 로직에 영향 없음 → 로그만 남기고 커밋
            ack.acknowledge();
        }
    }
}
