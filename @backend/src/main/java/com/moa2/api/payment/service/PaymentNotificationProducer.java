package com.moa2.api.payment.service;

import com.moa2.api.payment.dto.PaymentEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentNotificationProducer {    private static final String TOPIC = "payment-notification";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendPaymentNotification(String orderId, String email) {
        log.info("결제 알림 Kafka 이벤트 발행: orderId={}, email={}", orderId, email);
        PaymentEventDto event = new PaymentEventDto(orderId, email);
        kafkaTemplate.send(TOPIC, event);
    }
}
