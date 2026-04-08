package com.moa2.api.payment.service;

import com.moa2.api.payment.dto.PaymentEventDto;
import com.moa2.api.reservation.domain.entity.Payment;
import com.moa2.api.reservation.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentNotificationConsumer {

    private final JavaMailSender mailSender;
    private final PaymentRepository paymentRepository;

    @Transactional
    @KafkaListener(
        topics = "payment-notification",
        groupId = "payment-notification-group-2"
    )
    public void consumePaymentNotification(PaymentEventDto event, Acknowledgment ack) {
        log.info("결제 알림 Kafka 이벤트 수신: orderId={}, email={}", event.getOrderId(), event.getEmail());

        try {
            // 1. 이미 발송되었는지 확인 (중복 발송 방지)
            Optional<Payment> paymentOpt = paymentRepository.findByOrderId(event.getOrderId());
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                if (payment.getIsEmailSent() != null && payment.getIsEmailSent()) {
                    log.info("이미 이메일 발송이 완료된 결제입니다. 중복 발송을 방지합니다. orderId={}", event.getOrderId());
                    return; // finally 블록에서 ack.acknowledge() 실행됨
                }
            } else {
                log.warn("결제 정보를 찾을 수 없습니다. (하지만 이메일 발송은 시도합니다) orderId={}", event.getOrderId());
            }

            // 2. 이메일 발송 로직
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(event.getEmail());
            message.setSubject("주문완료 안내");
            
            String text = String.format("주문완료!\n주문확인: %s\n\n자세한 건 마이페이지에서 확인하세요", event.getOrderId());
            message.setText(text);
            
            mailSender.send(message);
            log.info("결제 완료 이메일 발송 성공: email={}", event.getEmail());

            // 3. 발송 성공 시 DB 업데이트
            paymentOpt.ifPresent(payment -> {
                payment.markEmailAsSent();
                paymentRepository.save(payment); // @Transactional 이므로 자동 더티체크 되지만 명시적 호출
            });

        } catch (Exception e) {
            log.error("결제 완료 이메일 발송 실패: email={}, error={}", event.getEmail(), e.getMessage());
        } finally {
            // 수동 커밋 (성공하든 실패하든 메시지를 다시 읽지 않도록 커밋)
            ack.acknowledge();
        }
    }
}
