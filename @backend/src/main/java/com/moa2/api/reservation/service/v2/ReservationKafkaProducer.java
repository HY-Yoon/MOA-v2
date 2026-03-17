package com.moa2.api.reservation.service.v2;

import com.moa2.api.reservation.dto.ReservationRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka 결제 완료 알림 이벤트 발행자
 * - 결제 완료 후 이메일/SMS 알림용 이벤트를 payment-notification 토픽에 발행
 */
@Slf4j
@Profile("v2")
@Component
@RequiredArgsConstructor
public class ReservationKafkaProducer {

    private static final String TOPIC = "payment-notification";

    private final KafkaTemplate<String, ReservationRequestEvent> kafkaTemplate;

    public void send(ReservationRequestEvent event) {
        log.info("결제 완료 알림 이벤트 발행 - eventId: {}, userId: {}",
                event.getEventId(), event.getUserId());
        kafkaTemplate.send(TOPIC, String.valueOf(event.getUserId()), event);
    }
}
