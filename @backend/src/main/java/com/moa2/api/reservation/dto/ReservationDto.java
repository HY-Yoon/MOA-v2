package com.moa2.api.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 예매 관련 DTO 모음 (Record 변환 완료)
 */
public class ReservationDto {

        /**
         * 예매 목록 응답 DTO
         */
        @Builder
        @Schema(name = "UserReservationListResponse", description = "예매 목록 응답")
        public record ListResponse(
                        @Schema(description = "예매 ID", example = "1") Long reservationId,

                        @Schema(description = "예매 번호", example = "R20260115-001") String reservationNumber,

                        @Schema(description = "예매일시", example = "2026-01-15T10:30:00") LocalDateTime reservationDate,

                        @Schema(description = "예매 상태", example = "CONFIRMED") String reservationStatus,

                        @Schema(description = "결제 상태", example = "COMPLETED") String paymentStatus,

                        @Schema(description = "공연 정보") ShowInfo show,

                        @Schema(description = "일정 정보") ScheduleInfo schedule,

                        @Schema(description = "좌석 수", example = "2") Integer seatCount,

                        @Schema(description = "총 결제 금액", example = "300000") Integer totalAmount,

                        @Schema(description = "취소 가능 여부 (공연 2일 전까지 가능)", example = "true") Boolean canCancel,

                        @Schema(description = "취소 마감 기한", example = "2026-02-02T19:30:00") LocalDateTime cancellationDeadline) {
                public static ListResponse from(com.moa2.api.reservation.domain.entity.Reservation reservation) {
                        return ListResponse.builder()
                                        .reservationId(reservation.getId())
                                        .reservationNumber(reservation.getReservationNumber())
                                        .reservationDate(reservation.getCreatedAt())
                                        .reservationStatus(reservation.getStatus().name())
                                        .paymentStatus(reservation.getPayment() != null
                                                        ? reservation.getPayment().getStatus().name()
                                                        : null)
                                        .show(ShowInfo.from(reservation.getShowSchedule().getShow()))
                                        .schedule(ScheduleInfo.from(reservation.getShowSchedule()))
                                        .seatCount(reservation.getSeatCount())
                                        .totalAmount(reservation.getTotalAmount())
                                        .canCancel(reservation.isCancellable())
                                        .cancellationDeadline(reservation.getCancellationDeadline())
                                        .build();
                }
        }

        /**
         * 예매 상세 응답 DTO
         */
        @Builder
        @Schema(name = "UserReservationDetailResponse", description = "예매 상세 응답")
        public record DetailResponse(
                        // ===== 예매 기본 정보 =====
                        @Schema(description = "예매 ID", example = "1") Long reservationId,

                        @Schema(description = "예매 번호", example = "R20260115-001") String reservationNumber,

                        @Schema(description = "예매일시", example = "2026-01-15T10:30:00") LocalDateTime reservationDate,

                        @Schema(description = "예매 상태", example = "CONFIRMED") String reservationStatus,

                        // ===== 공연 정보 =====
                        @Schema(description = "공연 정보") ShowInfo show,

                        // ===== 일정 정보 =====
                        @Schema(description = "일정 정보") ScheduleInfo schedule,

                        // ===== 좌석 정보 =====
                        @Schema(description = "좌석 목록") List<SeatInfo> seats,

                        @Schema(description = "좌석 수", example = "2") Integer seatCount,

                        // ===== 예매자 정보 =====
                        @Schema(description = "예매자 정보") BookerInfo booker,

                        // ===== 결제 정보 =====
                        @Schema(description = "결제 정보") PaymentInfo payment,

                        // ===== 취소 관련 정보 =====
                        @Schema(description = "취소 가능 여부 (공연 2일 전까지 가능)", example = "true") Boolean canCancel,

                        @Schema(description = "취소 마감 기한", example = "2026-02-02T19:30:00") LocalDateTime cancellationDeadline,

                        @Schema(description = "취소일시", example = "2026-01-18T09:00:00") LocalDateTime cancelledAt) {
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
                                        .payment(reservation.getPayment() != null
                                                        ? PaymentInfo.from(reservation.getPayment())
                                                        : null)
                                        .canCancel(reservation.isCancellable())
                                        .cancellationDeadline(reservation.getCancellationDeadline())
                                        .cancelledAt(reservation.getCancelledAt())
                                        .build();
                }
        }

        /**
         * 예매 취소 응답 DTO
         */
        @Builder
        @Schema(description = "예매 취소 응답")
        public record CancelResponse(
                        @Schema(description = "예매 ID", example = "1") Long reservationId,

                        @Schema(description = "예매 번호", example = "R20260115-001") String reservationNumber,

                        @Schema(description = "메시지", example = "예매가 취소되었습니다.") String message) {
        }

        // ===== Nested Records (공통 사용) =====

        /**
         * 공연 정보
         */
        @Builder
        @Schema(description = "공연 정보")
        public record ShowInfo(
                        @Schema(description = "공연 ID", example = "1") Long showId,

                        @Schema(description = "공연 제목", example = "레미제라블") String title,

                        @Schema(description = "포스터 URL", example = "/images/posters/lesmiserables.jpg") String posterUrl,

                        @Schema(description = "장르", example = "MUSICAL") String genre,

                        @Schema(description = "상영 시간", example = "150분") String runningTime,

                        @Schema(description = "출연진", example = "김철수, 이영희") String cast) {
                public static ShowInfo from(com.moa2.api.show.domain.entity.Show show) {
                        return ShowInfo.builder()
                                        .showId(show.getId())
                                        .title(show.getTitle())
                                        .posterUrl(show.getPosterUrl())
                                        .genre(show.getGenre().name())
                                        .runningTime(show.getRunningTime())
                                        .cast(show.getCast())
                                        .build();
                }
        }

        /**
         * 일정 정보
         */
        @Builder
        @Schema(description = "일정 정보")
        public record ScheduleInfo(
                        @Schema(description = "스케줄 ID", example = "1") Long scheduleId,

                        @Schema(description = "공연일", example = "2026-02-20") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate showDate,

                        @Schema(description = "공연 시간", example = "19:00") @JsonFormat(pattern = "HH:mm") LocalTime showTime,

                        @Schema(description = "공연 장소 정보") LocationInfo location) {
                public static ScheduleInfo from(com.moa2.api.show.domain.entity.ShowSchedule schedule) {
                        return ScheduleInfo.builder()
                                        .scheduleId(schedule.getId())
                                        .showDate(schedule.getShowDate())
                                        .showTime(schedule.getShowTime())
                                        .location(LocationInfo.from(schedule.getShow().getVenue()))
                                        .build();
                }
        }

        /**
         * 공연 장소 정보
         */
        @Builder
        @Schema(description = "공연 장소 정보")
        public record LocationInfo(
                        @Schema(description = "지역", example = "SEOUL") String region,

                        @Schema(description = "공연장명", example = "예술의전당") String venue,

                        @Schema(description = "홀명", example = "오페라극장") String hallName,

                        @Schema(description = "주소", example = "서울특별시 서초구 남부순환로 2406") String address) {
                public static LocationInfo from(com.moa2.api.show.domain.entity.Venue venue) {
                        return LocationInfo.builder()
                                        .region(venue.getRegion().name())
                                        .venue(venue.getName())
                                        .hallName(venue.getHallName())
                                        .address(venue.getAddress())
                                        .build();
                }
        }

        /**
         * 좌석 정보
         */
        @Builder
        public record SeatInfo(
                        @Schema(description = "구역명", example = "A구역") String sectionName,

                        @Schema(description = "행", example = "A") String row,

                        @Schema(description = "좌석 번호", example = "5") Integer number,

                        @Schema(description = "가격", example = "150000") Integer price) {

                public static SeatInfo from(com.moa2.api.reservation.domain.entity.ReservationSeat reservationSeat) {
                        var seat = reservationSeat.getSeat();
                        return SeatInfo.builder()
                                        .sectionName(seat.getSection().getName())
                                        .row(seat.getSeatRow())
                                        .number(seat.getSeatNumber())
                                        .price(reservationSeat.getPrice())
                                        .build();
                }
        }

        /**
         * 예매자 정보
         */
        @Builder
        @Schema(description = "예매자 정보")
        public record BookerInfo(
                        @Schema(description = "이름", example = "홍길동") String name,

                        @Schema(description = "전화번호", example = "010-1234-5678") String phone,

                        @Schema(description = "이메일", example = "todayda1006@gmail.com") String email) {
                public static BookerInfo from(com.moa2.api.reservation.domain.entity.Reservation reservation) {
                        return BookerInfo.builder()
                                        .name(reservation.getBookerName())
                                        .phone(reservation.getBookerPhone())
                                        .email(reservation.getBookerEmail())
                                        .build();
                }
        }

        /**
         * 결제 정보
         */
        @Builder
        @Schema(description = "결제 정보")
        public record PaymentInfo(
                        @Schema(description = "주문 ID", example = "ORD20260115-001") String orderId,

                        @Schema(description = "결제 키", example = "tviva20260115abc123") String paymentKey,

                        @Schema(description = "총 결제 금액", example = "300000") Integer totalAmount,

                        @Schema(description = "결제 방법", example = "CARD") String paymentMethod,

                        @Schema(description = "결제 상태", example = "COMPLETED") String paymentStatus,

                        @Schema(description = "결제 승인일시", example = "2026-01-15T10:31:00") LocalDateTime paidAt) {
                public static PaymentInfo from(com.moa2.api.reservation.domain.entity.Payment payment) {
                        return PaymentInfo.builder()
                                        .orderId(payment.getOrderId())
                                        .paymentKey(payment.getPaymentKey())
                                        .totalAmount(payment.getAmount())
                                        .paymentMethod(payment.getPaymentMethod() != null
                                                        ? payment.getPaymentMethod().name()
                                                        : null)
                                        .paymentStatus(payment.getStatus().name())
                                        .paidAt(payment.getApprovedAt())
                                        .build();
                }
        }
}