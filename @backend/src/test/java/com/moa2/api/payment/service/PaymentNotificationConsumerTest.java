package com.moa2.api.payment.service;

import com.moa2.api.payment.dto.PaymentEventDto;
import com.moa2.api.reservation.domain.entity.Payment;
import com.moa2.api.reservation.domain.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kafka 결제 알림 재수신·메일 장애 상황에서 중복 발송 또는 메시지 유실로 이어지는 사고를 막는다.
 */
@ExtendWith(MockitoExtension.class)
class PaymentNotificationConsumerTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private Acknowledgment ack;

    @Test
    @DisplayName("이미 이메일 발송 완료된 결제 메시지를 다시 받으면 메일을 보내지 않고 ack 한다")
    void 이미_이메일_발송_완료된_결제_메시지를_다시_받으면_메일을_보내지_않고_ack_한다() {
        // given
        PaymentNotificationConsumer consumer = new PaymentNotificationConsumer(mailSender, paymentRepository);
        Payment payment = Payment.builder()
                .orderId("order-1")
                .amount(50_000)
                .build();
        payment.markEmailAsSent();

        when(paymentRepository.findByOrderId("order-1")).thenReturn(Optional.of(payment));

        // when
        consumer.consumePaymentNotification(new PaymentEventDto("order-1", "user@example.com"), ack);

        // then
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("이메일 미발송 결제는 메일 발송 후 발송 완료 플래그를 true로 저장한다")
    void 이메일_미발송_결제는_메일_발송_후_발송_완료_플래그를_true로_저장한다() {
        // given
        PaymentNotificationConsumer consumer = new PaymentNotificationConsumer(mailSender, paymentRepository);
        Payment payment = Payment.builder()
                .orderId("order-1")
                .amount(50_000)
                .build();

        when(paymentRepository.findByOrderId("order-1")).thenReturn(Optional.of(payment));

        // when
        consumer.consumePaymentNotification(new PaymentEventDto("order-1", "user@example.com"), ack);

        // then
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getTo()).containsExactly("user@example.com");
        assertThat(payment.getIsEmailSent()).isTrue();
        verify(paymentRepository).save(payment);
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("메일 발송이 실패해도 현재 코드는 ack를 호출한다")
    void 메일_발송이_실패해도_현재_코드는_ack를_호출한다() {
        // given
        PaymentNotificationConsumer consumer = new PaymentNotificationConsumer(mailSender, paymentRepository);
        Payment payment = Payment.builder()
                .orderId("order-1")
                .amount(50_000)
                .build();

        when(paymentRepository.findByOrderId("order-1")).thenReturn(Optional.of(payment));
        doThrow(new RuntimeException("SMTP 장애")).when(mailSender).send(any(SimpleMailMessage.class));

        // when
        consumer.consumePaymentNotification(new PaymentEventDto("order-1", "user@example.com"), ack);

        // then
        assertThat(payment.getIsEmailSent()).isFalse();
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(ack).acknowledge();
    }
}
