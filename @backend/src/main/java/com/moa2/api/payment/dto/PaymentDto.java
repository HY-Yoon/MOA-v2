package com.moa2.api.payment.dto;

import com.moa2.api.user.domain.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
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
         */
        @Schema(description = "결제 정보")
        public record PaymentInfo(
                        @Schema(description = "결제 수단", example = "CARD") String method,

                        @Schema(description = "결제 금액", example = "150000") Long amount,

                        @Schema(description = "결제 일시", example = "2024-01-01T12:00:00") LocalDateTime paidAt) {
        }

        /**
         * 결제 요청 DTO
         */
        @Schema(description = "결제 요청 정보")
        public record Request(
                        @Schema(description = "스케줄 ID", example = "1") @NotNull(message = "스케줄 ID는 필수입니다") Long scheduleId,

                        @Schema(description = "선점할 스케줄 좌석 ID 목록", example = "[4901, 4902]") @NotEmpty(message = "스케줄 좌석 ID 목록은 필수입니다") List<Long> scheduleSeatIds,

                        @Schema(description = "예매자 이름", example = "홍길동") @NotBlank(message = "예매자 이름은 필수입니다") String bookerName,

                        @Schema(description = "예매자 연락처", example = "010-1234-5678") @NotBlank(message = "예매자 연락처는 필수입니다") String bookerPhone,

                        @Schema(description = "예매자 이메일", example = "hong@example.com") String bookerEmail) {
        }

        /**
         * 결제 요청 응답 DTO
         */
        @Schema(description = "결제 요청 응답 정보")
        public record RequestResponse(
                        @Schema(description = "주문 번호 (UUID)", example = "550e8400-e29b-41d4-a716-446655440000") String orderId,

                        @Schema(description = "결제 금액", example = "150000") Integer amount,

                        @Schema(description = "주문명", example = "뮤지컬 캣츠 - 2좌석") String orderName,

                        @Schema(description = "예매자 정보") BookerInfo booker,

                        @Schema(description = "결제 성공 리다이렉트 URL", example = "http://localhost:5173/payment/success") String successUrl,

                        @Schema(description = "결제 실패 리다이렉트 URL", example = "http://localhost:5173/payment/fail") String failUrl) {
                @Schema(description = "예매자 상세 정보")
                public record BookerInfo(
                                @Schema(description = "이름", example = "홍길동") String name,

                                @Schema(description = "이메일", example = "hong@example.com") String email,

                                @Schema(description = "연락처", example = "010-1234-5678") String phone) {
                }
        }

        /**
         * 결제 승인 요청 DTO
         */
        @Schema(description = "결제 승인 요청 정보")
        public record ConfirmRequest(
                        @Schema(description = "결제 키", example = "test_payment_key") @NotBlank(message = "paymentKey는 필수입니다") String paymentKey,

                        @Schema(description = "주문 번호", example = "550e8400-e29b-41d4-a716-446655440000") @NotBlank(message = "orderId는 필수입니다") String orderId,

                        @Schema(description = "결제 금액", example = "150000") @NotNull(message = "amount는 필수입니다") Long amount) {
        }

        /**
         * 결제 승인 성공 응답 DTO
         */
        @Schema(description = "결제 승인 성공 응답")
        public record SuccessResponse(
                        @Schema(description = "예약 ID", example = "100") Long reservationId,

                        @Schema(description = "예약 번호", example = "BOOK-1234567890") String reservationNumber,

                        @Schema(description = "주문 번호", example = "550e8400-e29b-41d4-a716-446655440000") String orderId,

                        @Schema(description = "결제 키", example = "test_payment_key") String paymentKey,

                        @Schema(description = "결제 금액", example = "150000") Integer amount,

                        @Schema(description = "결제 수단", example = "CARD") String method,

                        @Schema(description = "주문명", example = "뮤지컬 캣츠 - 2좌석") String orderName,

                        @Schema(description = "승인 일시", example = "2024-01-01T12:05:00") LocalDateTime approvedAt) {
        }

        /**
         * 결제 실패 요청 DTO
         */
        @Schema(description = "결제 실패 요청 정보")
        public record FailRequest(
                        @Schema(description = "주문 번호", example = "550e8400-e29b-41d4-a716-446655440000") String orderId,

                        @Schema(description = "에러 코드", example = "PAY_PROCESS_CANCELED") String code,

                        @Schema(description = "에러 메시지", example = "사용자에 의해 결제가 취소되었습니다.") String message) {
        }

        /**
         * 결제 완료 시 내려줄 최종 응답 DTO
         */
        @Schema(description = "결제 완료 상세 정보")
        public record PaymentSuccessResponse(
                        @Schema(description = "예약 ID", example = "booking_123") String reservationId,

                        @Schema(description = "공연 정보") PerformanceInfo performance,

                        @Schema(description = "좌석 정보 목록") List<SeatInfo> seats,

                        @Schema(description = "예매자 정보") BookerInfo booker,

                        @Schema(description = "결제 정보") PaymentInfo payment) {
                public PaymentSuccessResponse {
                        seats = seats == null ? List.of() : List.copyOf(seats);
                }

                @Schema(description = "공연 상세 정보")
                public record PerformanceInfo(
                                @Schema(description = "공연 제목", example = "캣츠") String title,

                                @Schema(description = "공연 일시", example = "2024-02-20T19:00:00") LocalDateTime date,

                                @Schema(description = "회차", example = "1") Integer session) {
                }

                @Schema(description = "좌석 상세 정보")
                public record SeatInfo(
                                @Schema(description = "구역명", example = "VIP") String section,

                                @Schema(description = "좌석 번호", example = "A-12") String seatNumber) {
                }

                @Schema(description = "예매자 상세 정보")
                public record BookerInfo(
                                @Schema(description = "이름", example = "홍길동") String name,

                                @Schema(description = "연락처", example = "010-1234-5678") String phone,

                                @Schema(description = "이메일", example = "hong@example.com") String email) {
                }
        }

        /**
         * 결제 완료 페이지용 응답 DTO
         */
        @Schema(description = "결제 완료 페이지 응답 정보")
        public record CompletionResponse(
                        @Schema(description = "예매 번호", example = "BOOK-20240201-XXXX") String reservationNumber,

                        @Schema(description = "주문 번호", example = "550e8400-e29b-41d4-a716-446655440000") String orderId,

                        @Schema(description = "공연 정보") CompletionPerformanceInfo performance,

                        @Schema(description = "좌석 정보 목록") List<CompletionSeatInfo> seats,

                        @Schema(description = "예매자 정보") CompletionBookerInfo booker,

                        @Schema(description = "결제 정보") PaymentInfo payment) {
                public CompletionResponse {
                        seats = seats == null ? List.of() : List.copyOf(seats);
                }

                @Schema(description = "완료 페이지 공연 정보")
                public record CompletionPerformanceInfo(
                                @Schema(description = "공연 제목", example = "캣츠") String title,

                                @Schema(description = "공연 날짜", example = "2024-02-20") String date,

                                @Schema(description = "공연 시간", example = "19:00") String showTime,

                                @Schema(description = "회차", example = "1") Integer session) {
                }

                @Schema(description = "완료 페이지 좌석 정보")
                public record CompletionSeatInfo(
                                @Schema(description = "구역명", example = "VIP") String section,

                                @Schema(description = "좌석 번호", example = "A-12") String seatNumber) {
                }

                @Schema(description = "완료 페이지 예매자 정보")
                public record CompletionBookerInfo(
                                @Schema(description = "이름", example = "홍길동") String name,

                                @Schema(description = "연락처", example = "010-1234-5678") String phone,

                                @Schema(description = "이메일", example = "hong@example.com") String email) {
                }
        }

        /**
         * 실패 시 프론트 리다이렉트 URL 쿼리 파라미터
         */
        @Schema(description = "실패 리다이렉트 파라미터")
        public record FailRedirectParams(
                        @Schema(description = "에러 코드", example = "PAY_PROCESS_CANCELED") String code,

                        @Schema(description = "에러 메시지", example = "사용자에 의해 취소되었습니다.") String message,

                        @Schema(description = "주문 번호", example = "550e8400-e29b-41d4-a716-446655440000") String orderId) {
        }

        /**
         * 백엔드만 테스트 시 GET /fail?noRedirect=1 응답
         */
        @Schema(description = "테스트용 리다이렉트 방지 실패 응답")
        public record TestNoRedirectFailResponse(
                        @Schema(description = "에러 코드", example = "PAY_PROCESS_CANCELED") String code,

                        @Schema(description = "에러 메시지", example = "사용자에 의해 취소되었습니다.") String message,

                        @Schema(description = "주문 번호", example = "550e8400-e29b-41d4-a716-446655440000") String orderId,

                        @Schema(description = "리다이렉트 예정 URL", example = "http://localhost:5173/payment/fail?...") String redirectUrl) {
        }
}