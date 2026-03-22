package com.moa2.api.payment.service;

import com.moa2.api.payment.dto.PaymentEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentNotificationConsumer {

    private final JavaMailSender mailSender;    @KafkaListener(
        topics = "payment-notification", 
        groupId = "payment-notification-group-2",
        properties = {"auto.offset.reset=latest"}
    )
    public void consumePaymentNotification(PaymentEventDto event) {
        log.info("결제 알림 Kafka 이벤트 수신: orderId={}, email={}", event.getOrderId(), event.getEmail());
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(event.getEmail());
            message.setSubject("주문완료 안내");
            
            String text = String.format("주문완료!\n주문확인: %s\n\n자세한 건 마이페이지에서 확인하세요", event.getOrderId());
            message.setText(text);
            
            mailSender.send(message);
            log.info("결제 완료 이메일 발송 성공: email={}", event.getEmail());
        } catch (Exception e) {
            log.error("결제 완료 이메일 발송 실패: email={}, error={}", event.getEmail(), e.getMessage());
        }
    }
}
