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
                        LocalDateTime paidAt) {
        }

        /**
         * 결제 요청 DTO
         * 프론트가 토스 위젯을 띄우기 전, 사전 데이터를 생성하기 위한 요청
         */
        public record Request(
                        @NotNull(message = "스케줄 ID는 필수입니다") Long scheduleId,

                        @NotEmpty(message = "좌석 ID 목록은 필수입니다") List<Long> seatIds,

                        @NotBlank(message = "예매자 이름은 필수입니다") String bookerName,

                        @NotBlank(message = "예매자 연락처는 필수입니다") String bookerPhone,

                        String bookerEmail) {
        }

        /**
         * 결제 요청 응답 DTO
         * 프론트가 토스 위젯에 전달할 정보
         */
        public record RequestResponse(
                        String orderId, // UUID 기반 주문번호
                        Integer amount, // 결제 금액
                        String orderName, // "뮤지컬 XX - 2좌석" 형식
                        BookerInfo booker, // 예매자 정보
                        String successUrl, // 결제 성공 시 리다이렉트 URL
                        String failUrl // 결제 실패 시 리다이렉트 URL
        ) {
                /**
                 * 예매자 정보
                 */
                public record BookerInfo(
                                String name,
                                String email,
                                String phone) {
                }
        }

        /**
         * 결제 승인 요청 DTO
         * 토스 successUrl에서 받은 파라미터
         */
        public record ConfirmRequest(
                        @NotBlank(message = "paymentKey는 필수입니다") String paymentKey,

                        @NotBlank(message = "orderId는 필수입니다") String orderId,

                        @NotNull(message = "amount는 필수입니다") Long amount) {
        }

        /**
         * 결제 승인 성공 응답 DTO
         */
        public record SuccessResponse(
                        Long reservationId, // 예약 ID
                        String reservationNumber, // 예약 번호
                        String orderId, // 주문 번호
                        String paymentKey, // 토스 결제 키
                        Integer amount, // 결제 금액
                        String method, // 결제 수단
                        String orderName, // 주문명
                        LocalDateTime approvedAt // 승인 일시
        ) {
        }

        /**
         * 결제 실패 요청 DTO
         * 토스 failUrl에서 받은 파라미터
         */
        public record FailRequest(
                        String orderId, // 주문 번호 (없을 수 있음 - PAY_PROCESS_CANCELED 케이스)
                        String code, // 에러 코드
                        String message // 에러 메시지
        ) {
        }

        /**
         * 결제 완료 시 내려줄 최종 응답 DTO (Record 방식)
         */
        public record PaymentSuccessResponse(
                        String reservationId,
                        PerformanceInfo performance, // 내부 record 참조
                        List<SeatInfo> seats, // 내부 record 참조
                        BookerInfo booker, // 예매자 정보
                        PaymentInfo payment) {
                public PaymentSuccessResponse {
                        seats = seats == null ? List.of() : List.copyOf(seats);
                }

                /**
                 * 공연 정보
                 */
                public record PerformanceInfo(
                                String title,
                                LocalDateTime date,
                                Integer session) { // 회차 (같은 날짜의 몇 번째 회차인지)
                }

                /**
                 * 좌석 정보
                 */
                public record SeatInfo(
                                String section,
                                String seatNumber) {
                }

                /**
                 * 예매자 정보
                 */
                public record BookerInfo(
                                String name,
                                String phone,
                                String email) {
                }
        }

        /**
         * 예매자 확인 정보 응답 DTO
         * 결제 과정 중 예매자 확인 단계에서 사용
         */
        public record BuyerInfoResponse(
                        String name, // 예매자 이름
                        String email, // 예매자 이메일
                        String phone // 예매자 연락처 (nullable)
        ) {
        }

        // ------------------------- 결제 완료 / 실패 페이지 (핸들러 연동) -------------------------

        /**
         * 결제 완료 페이지용 응답 DTO (예매 완료 정보 노출)
         * GET /api/v1/payment/complete?reservationNumber=xxx 응답.
         * 스펙: 공연 기본 정보, 예매자 정보, 결제 정보. 엔티티명 유지 (reservationNumber, orderId 등).
         */
        public record CompletionResponse(
                        String reservationNumber, // 예매번호 (bookingId)
                        String orderId, // 주문 번호
                        CompletionPerformanceInfo performance,
                        List<CompletionSeatInfo> seats,
                        CompletionBookerInfo booker,
                        PaymentInfo payment) {
                public CompletionResponse {
                        seats = seats == null ? List.of() : List.copyOf(seats);
                }

                public record CompletionPerformanceInfo(
                                String title,
                                String date, // yyyy-MM-dd
                                String showTime, // HH:mm
                                Integer session) { // 회차 (같은 날짜의 몇 번째 회차인지)
                }

                public record CompletionSeatInfo(
                                String section, // 구역명
                                String seatNumber // 좌석번호 (row-number 등)
                ) {
                }

                public record CompletionBookerInfo(
                                String name,
                                String phone,
                                String email) {
                }
        }

        /**
         * 실패 시 프론트 리다이렉트 URL 쿼리 파라미터 (문서/구성용)
         * GET /fail 핸들러가 frontend-fail-url 로 redirect 시
         * ?code=...&message=...&orderId=...
         */
        public record FailRedirectParams(
                        String code,
                        String message,
                        String orderId // 없을 수 있음 (PAY_PROCESS_CANCELED 등)
        ) {
        }

        /**
         * 백엔드만 테스트 시 GET /fail?noRedirect=1 응답 (302 대신 200 JSON)
         */
        public record TestNoRedirectFailResponse(
                        String code,
                        String message,
                        String orderId,
                        String redirectUrl) {
        }

}