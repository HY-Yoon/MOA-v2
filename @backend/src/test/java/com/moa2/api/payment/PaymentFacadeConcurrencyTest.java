package com.moa2.api.payment;

import com.moa2.api.payment.facade.PaymentFacade;
import com.moa2.api.reservation.domain.entity.Payment;
import com.moa2.api.reservation.domain.entity.Reservation;
import com.moa2.api.reservation.domain.repository.PaymentRepository;
import com.moa2.api.reservation.domain.repository.ReservationRepository;
import com.moa2.api.show.domain.entity.ShowSchedule;
import com.moa2.api.show.domain.repository.ShowScheduleRepository;
import com.moa2.api.user.domain.entity.User;
import com.moa2.api.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class PaymentFacadeConcurrencyTest {
    @Autowired private PaymentFacade paymentFacade;
    @Autowired private UserRepository userRepository;
    @Autowired private ShowScheduleRepository showScheduleRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private PaymentRepository paymentRepository;

    // 테스트에 쓸 변수들
    private String orderId;
    private Long amount;

    @BeforeEach
    void setUp() {
        // 1. 유저 생성
        User user = userRepository.save(User.builder().email("test@test.com").build());

        // 2. 스케줄 생성
        ShowSchedule schedule = showScheduleRepository.save(ShowSchedule.builder().build());

        // 3. 예약 & 결제(PENDING) 데이터 생성
        this.orderId = "TEST-ORDER-" + UUID.randomUUID();
        this.amount = 10000L;

        Reservation reservation = reservationRepository.save(
                Reservation.builder()
                        .user(user)
                        .showSchedule(schedule)
                        .reservationNumber("RES-" + UUID.randomUUID())
                        .totalAmount(amount.intValue())
                        .seatCount(1)
                        .bookerName("테스트")
                        .bookerPhone("010-1234-5678")
                        .bookerEmail("test@test.com")
                        .build()
        );

        paymentRepository.save(
                Payment.builder()
                        .reservation(reservation)
                        .orderId(this.orderId)
                        .amount(this.amount.intValue())
                        .build()
        );
    }

    @Test
    @DisplayName("동시에 같은 주문을 결제 승인 요청하면, 1개만 성공하고 나머지는 실패해야 한다.")
    void confirmPayment_concurrency_test() throws InterruptedException {
        // 1. [Given] 데이터 준비
        // @BeforeEach에서 이미 PENDING 상태의 결제 데이터를 생성했음
        String paymentKey = "test_toss_key";

        // 2. [When] 동시 요청 환경 구성
        int threadCount = 2; // 동시에 2명이 따닥!
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    // Facade의 confirmPaymentMock 호출 (Toss 연동 없이 락 테스트만 할 거면 Mock 메서드 사용 추천)
                    paymentFacade.confirmPaymentMock(paymentKey, orderId, amount);

                    successCount.getAndIncrement();
                } catch (Exception e) {
                    // "이미 진행 중입니다" or "완료된 결제입니다" 등의 예외 발생 예상
                    System.out.println("결제 실패: " + e.getMessage());
                    failCount.getAndIncrement();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 두 스레드가 끝날 때까지 대기

        // 3. [Then] 검증
        // 하나는 성공, 하나는 실패해야 함
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(1);
    }
}
