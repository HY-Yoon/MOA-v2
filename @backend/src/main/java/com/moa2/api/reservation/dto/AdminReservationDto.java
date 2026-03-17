package com.moa2.api.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.moa2.api.reservation.dto.ReservationDto.BookerInfo;
import com.moa2.api.reservation.dto.ReservationDto.PaymentInfo;
import com.moa2.api.reservation.dto.ReservationDto.ScheduleInfo;
import com.moa2.api.reservation.dto.ReservationDto.SeatInfo;
import com.moa2.api.reservation.dto.ReservationDto.ShowInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

import com.moa2.global.model.PaymentStatus;
import com.moa2.global.model.ReservationStatus;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 관리자용 예매 관련 DTO
 */
public class AdminReservationDto {

        /**
         * 예매 목록 검색 조건
         */
        @Builder
        @Schema(description = "예매 목록 검색 조건")
        public record SearchCondition(
                        @Parameter(description = "검색 유형") ReservationSearchType searchType,

                        @Parameter(description = "검색어 (예매번호, 예매자명, 예매자ID, 제목)") String searchKeyword,

                        @Parameter(description = "예매 상태") ReservationStatus status,

                        @Parameter(description = "결제 상태") PaymentStatus paymentStatus,

                        @Parameter(description = "공연 ID (해당 공연 기준 목록 조회)", example = "1") Long showId,

                        @Parameter(hidden = true) Long scheduleId,

                        @Parameter(description = "조회 시작일시") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,

                        @Parameter(description = "조회 종료일시") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,

                        @Parameter(description = "날짜 조회 기준 (RESERVATION_DATE: 예매일, SHOW_DATE: 공연일)") DateSearchType dateSearchType,

                        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") int page,

                        @Parameter(description = "페이지 크기", example = "10") int size) {
                public SearchCondition {
                        // 기본값 설정
                        if (size == 0)
                                size = 10;
                        if (dateSearchType == null)
                                dateSearchType = DateSearchType.RESERVATION_DATE;
                }
        }

        public enum ReservationSearchType {
                RESERVATION_NUMBER, BOOKER_NAME, BOOKER_ID, SHOW_TITLE
        }

        public enum DateSearchType {
                RESERVATION_DATE, SHOW_DATE
        }

        /**
         * 관리자용 예매 목록 응답
         */
        @Builder
        @Schema(name = "AdminReservationListResponse", description = "관리자용 예매 목록 응답")
        public record ListResponse(
                        @Schema(description = "예매 ID", example = "1") Long reservationId,

                        @Schema(description = "예매 번호", example = "R20260115-001") String reservationNumber,

                        @Schema(description = "예매일시", example = "2026-01-15T10:30:00") @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime reservationDate,

                        @Schema(description = "예매 상태", example = "CONFIRMED") String reservationStatus,

                        @Schema(description = "결제 상태", example = "COMPLETED") String paymentStatus,

                        @Schema(description = "공연 제목", example = "레미제라블") String showTitle,

                        @Schema(description = "일정 정보") ScheduleInfo schedule,

                        @Schema(description = "좌석 수", example = "2") Integer seatCount,

                        @Schema(description = "총 결제 금액", example = "300000") Integer totalAmount,

                        @Schema(description = "예매자명", example = "홍길동") String bookerName,

                        @Schema(description = "예매자 ID (이메일)", example = "user@example.com") String bookerId) {
                public static ListResponse from(com.moa2.api.reservation.domain.entity.Reservation reservation) {
                        return ListResponse.builder()
                                        .reservationId(reservation.getId())
                                        .reservationNumber(reservation.getReservationNumber())
                                        .reservationDate(reservation.getCreatedAt())
                                        .reservationStatus(reservation.getStatus().name())
                                        .paymentStatus(reservation.getPayment() != null
                                                        ? reservation.getPayment().getStatus().name()
                                                        : null)
                                        .showTitle(reservation.getShowSchedule().getShow().getTitle())
                                        .schedule(ScheduleInfo.from(reservation.getShowSchedule()))
                                        .seatCount(reservation.getSeatCount())
                                        .totalAmount(reservation.getTotalAmount())
                                        .bookerName(reservation.getUser().getName())
                                        .bookerId(reservation.getUser().getEmail())
                                        .build();
                }
        }

        /**
         * 관리자용 예매 상세 응답
         */
        @Builder
        @Schema(name = "AdminReservationDetailResponse", description = "관리자용 예매 상세 응답")
        public record DetailResponse(
                        @Schema(description = "예매 ID", example = "1") Long reservationId,

                        @Schema(description = "예매 번호", example = "R20260115-001") String reservationNumber,

                        @Schema(description = "예매일시", example = "2026-01-15T10:30:00") @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime reservationDate,

                        @Schema(description = "예매 상태", example = "CONFIRMED") String reservationStatus,

                        @Schema(description = "공연 정보") ShowInfo show,

                        @Schema(description = "일정 정보") ScheduleInfo schedule,

                        @Schema(description = "좌석 목록") List<SeatInfo> seats,

                        @Schema(description = "좌석 수", example = "2") Integer seatCount,

                        @Schema(description = "예매자 정보") BookerInfo booker,

                        @Schema(description = "예매자 ID (이메일)", example = "user@example.com") String bookerId,

                        @Schema(description = "결제 정보") PaymentInfo payment,

                        @Schema(description = "취소일시", example = "2026-01-18T09:00:00") @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime cancelledAt) {
                public static DetailResponse from(com.moa2.api.reservation.domain.entity.Reservation reservation,
                                List<com.moa2.api.reservation.domain.entity.ReservationSeat> reservationSeats) {
                        return DetailResponse.builder()
                                        .reservationId(reservation.getId())
                                        .reservationNumber(reservation.getReservationNumber())
                                        .reservationDate(reservation.getCreatedAt())
                                        .reservationStatus(reservation.getStatus().name())
                                        .show(ShowInfo.from(reservation.getShowSchedule().getShow()))
                                        .schedule(ScheduleInfo.from(reservation.getShowSchedule()))
                                        .seats(reservationSeats.stream().map(SeatInfo::from).toList())
                                        .seatCount(reservation.getSeatCount())
                                        .booker(BookerInfo.from(reservation))
                                        .bookerId(reservation.getUser().getEmail())
                                        .payment(reservation.getPayment() != null
                                                        ? PaymentInfo.from(reservation.getPayment())
                                                        : null)
                                        .cancelledAt(reservation.getCancelledAt())
                                        .build();
                }
        }
}
