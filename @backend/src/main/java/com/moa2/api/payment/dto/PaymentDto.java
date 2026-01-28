package com.moa2.api.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 결제 관련 DTO 통합 클래스
 */
public class PaymentDto {

    /**
     * 결제 정보 (결제 완료 응답에 포함)
     * method, amount, paidAt
     */
    public record PaymentInfo(
            String method,
            Long amount,
            LocalDateTime paidAt
    ) {}

    /**
     * 결제 요청 DTO
     * 프론트가 토스 위젯을 띄우기 전, 사전 데이터를 생성하기 위한 요청
     */
    public record Request(
        @NotNull(message = "스케줄 ID는 필수입니다")
        Long scheduleId,

        @NotEmpty(message = "좌석 ID 목록은 필수입니다")
        List<Long> seatIds,

        @NotBlank(message = "예매자 이름은 필수입니다")
        String bookerName,

        @NotBlank(message = "예매자 연락처는 필수입니다")
        String bookerPhone,

        String bookerEmail
    ) {}

    /**
     * 결제 요청 응답 DTO
     * 프론트가 토스 위젯에 전달할 정보
     */
    public record RequestResponse(
        String orderId,           // UUID 기반 주문번호
        Integer amount,           // 결제 금액
        String orderName,         // "뮤지컬 XX - 2좌석" 형식
        String customerName,      // 예매자 이름
        String customerEmail,     // 예매자 이메일
        String customerMobilePhone, // 예매자 연락처
        String successUrl,        // 결제 성공 시 리다이렉트 URL
        String failUrl            // 결제 실패 시 리다이렉트 URL
    ) {}

    /**
     * 결제 승인 요청 DTO
     * 토스 successUrl에서 받은 파라미터
     */
    public record ConfirmRequest(
        @NotBlank(message = "paymentKey는 필수입니다")
        String paymentKey,

        @NotBlank(message = "orderId는 필수입니다")
        String orderId,

        @NotNull(message = "amount는 필수입니다")
        Long amount
    ) {}

    /**
     * 결제 승인 성공 응답 DTO
     */
    public record SuccessResponse(
        Long reservationId,         // 예약 ID
        String reservationNumber,   // 예약 번호
        String orderId,             // 주문 번호
        String paymentKey,          // 토스 결제 키
        Integer amount,             // 결제 금액
        String method,              // 결제 수단
        String orderName,           // 주문명
        LocalDateTime approvedAt    // 승인 일시
    ) {}

    /**
     * 결제 실패 요청 DTO
     * 토스 failUrl에서 받은 파라미터
     */
    public record FailRequest(
        String orderId,    // 주문 번호 (없을 수 있음 - PAY_PROCESS_CANCELED 케이스)
        String code,       // 에러 코드
        String message     // 에러 메시지
    ) {}

    /**
     * 결제 완료 시 내려줄 최종 응답 DTO (Record 방식)
     */
    public record PaymentSuccessResponse(
            String reservationId,
            PerformanceInfo performance, // 내부 record 참조
            List<SeatInfo> seats,        // 내부 record 참조
            BookerInfo orderName,        // 내부 record 참조 (변수명 orderName 확인!)
            PaymentInfo payment
    ) {
        public PaymentSuccessResponse {
            seats = seats == null ? List.of() : List.copyOf(seats);
        }

        /**
         * 공연 정보
         */
        public record PerformanceInfo(
                String title,
                LocalDateTime date,
                Integer round
        ) {}

        /**
         * 좌석 정보
         */
        public record SeatInfo(
                String section,
                String seatNumber
        ) {}

        /**
         * 예매자 정보
         */
        public record BookerInfo(
                String name,
                String phoneNumber
        ) {}
    }

}