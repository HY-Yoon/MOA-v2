package com.moa2.api.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * V2: Redis 기반 예매 DTO
 * - 좌석 선점 (Redis) → 주문 생성 (DB) → 결제 완료 (Toss)
 */
public class ReservationDtoV2 {

    /**
     * [1단계] 좌석 선점 요청 DTO
     * - 좌석 선택 → "선택 완료" 버튼 클릭 시
     */
    @Builder
    @Schema(description = "V2 좌석 선점 요청 (토큰 필수)")
    public record ReserveRequest(
            @NotNull(message = "스케줄 ID는 필수입니다") @Schema(description = "공연 회차 ID", example = "7", requiredMode = Schema.RequiredMode.REQUIRED) Long scheduleId,

            @NotEmpty(message = "좌석을 선택해주세요") @Schema(description = "선택한 회차별 좌석 ID 목록 (scheduleSeatId)", example = "[1, 2]", requiredMode = Schema.RequiredMode.REQUIRED) List<Long> scheduleSeatIds) {
    }

    /**
     * [1단계] 좌석 선점 응답 DTO
     * - Redis SETNX 성공 시 반환
     * - DB Write 없음
     */
    @Builder
    @Schema(description = "V2 좌석 선점 응답")
    public record ReserveSeatResponse(
            @Schema(description = "선점된 좌석 수", example = "2") int seatCount,

            @Schema(description = "선점 남은 시간(초)", example = "300") long remainingSeconds,

            @Schema(description = "총 금액", example = "132000") int totalAmount,

            @Schema(description = "메시지", example = "좌석 선점 완료! 5분 내에 결제를 진행해주세요.") String message) {

        public static ReserveSeatResponse of(int seatCount, long remainingSeconds, int totalAmount) {
            return ReserveSeatResponse.builder()
                    .seatCount(seatCount)
                    .remainingSeconds(remainingSeconds)
                    .totalAmount(totalAmount)
                    .message("좌석 선점 완료! " + (remainingSeconds / 60) + "분 내에 결제를 진행해주세요.")
                    .build();
        }
    }

    /**
     * [2단계] 주문 생성 요청 DTO
     * - 예약자 정보 입력 → "결제하기" 버튼 클릭 시
     */
    @Builder
    @Schema(description = "V2 주문 생성 요청 (예약자 정보 포함)")
    public record CreateOrderRequest(
            @NotNull(message = "스케줄 ID는 필수입니다") @Schema(description = "공연 회차 ID", example = "7") Long scheduleId,

            @NotEmpty(message = "좌석을 선택해주세요") @Schema(description = "선점한 좌석 ID 목록 (scheduleSeatId)", example = "[1, 2]") List<Long> scheduleSeatIds,

            @NotBlank(message = "예약자 이름은 필수입니다") @Schema(description = "예약자 이름", example = "홍길동") String bookerName,

            @NotBlank(message = "예약자 연락처는 필수입니다") @Schema(description = "예약자 연락처", example = "010-1234-5678") String bookerPhone,

            @Schema(description = "예약자 이메일", example = "hong@example.com") String bookerEmail) {
    }

    /**
     * [2단계] 주문 생성 응답 DTO
     * - Toss 위젯 초기화에 필요한 정보 반환
     */
    @Builder
    @Schema(description = "V2 주문 생성 응답 (결제 진입용)")
    public record CreateOrderResponse(
            @Schema(description = "주문 번호 (Toss용)", example = "MOA-abc123def456ghi789jk") String orderId,

            @Schema(description = "총 결제 금액", example = "136000") int totalAmount,

            @Schema(description = "주문명", example = "뮤지컬 <ANNE> 10th Anniversary - 2좌석") String orderName,

            @Schema(description = "결제 기한", example = "2026-03-06T23:05:00") LocalDateTime paymentDeadline,

            @Schema(description = "결제 성공 리다이렉트 URL") String successUrl,

            @Schema(description = "결제 실패 리다이렉트 URL") String failUrl,

            @Schema(description = "예약자 정보") BookerInfo booker) {

        @Schema(description = "예약자 상세 정보")
        public record BookerInfo(
                @Schema(description = "이름", example = "홍길동") String name,
                @Schema(description = "이메일", example = "hong@example.com") String email,
                @Schema(description = "연락처", example = "010-1234-5678") String phone) {
        }
    }

    /**
     * [미리보기] 결제 페이지 진입 시 주문 상세 조회 응답 DTO
     */
    @Builder
    @Schema(description = "V2 주문 미리보기 응답 (결제 화면용)")
    public record PreviewResponse(
            @Schema(description = "공연 제목", example = "뮤지컬 <ANNE> 10th Anniversary") String title,
            @Schema(description = "장소", example = "대학로 자유극장") String venueName,
            @Schema(description = "공연 날짜", example = "2025-12-09") String showDate,
            @Schema(description = "공연 시간", example = "19:30") String showTime,
            @Schema(description = "예매 수수료 (총액)", example = "4000") int bookingFee,
            @Schema(description = "순수 티켓 금액 (총액)", example = "132000") int ticketAmount,
            @Schema(description = "총 결제 예정 금액 (티켓 + 수수료)", example = "136000") int totalAmount,
            @Schema(description = "결제 기한 (선점 만료 시각)", example = "2026-03-06T23:55:00") LocalDateTime paymentDeadline,
            @Schema(description = "선택한 좌석 목록") List<SeatPreviewInfo> seats,
            @Schema(description = "기본 예약자 정보 (로그인 사용자 정보)") BookerInfo defaultBooker) {

        @Builder
        @Schema(description = "상세 좌석 정보")
        public record SeatPreviewInfo(
                @Schema(description = "스케줄 좌석 ID", example = "3701") Long scheduleSeatId,
                @Schema(description = "등급 (구역명)", example = "R석") String gradeName,
                @Schema(description = "좌석 번호", example = "B열 17번") String seatNumber,
                @Schema(description = "가격", example = "66000") int price) {
        }

        @Builder
        @Schema(description = "기본 예약자 정보")
        public record BookerInfo(
                @Schema(description = "이름", example = "홍길동") String name,
                @Schema(description = "이메일", example = "hong@example.com") String email,
                @Schema(description = "연락처", example = "010-1234-5678") String phone) {
        }
    }
}
