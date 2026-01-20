package com.moa2.api.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 예매 상세 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "예매 상세 응답")
public class ReservationDetailResponse {
    
    // ===== 예매 기본 정보 =====
    @Schema(description = "예매 ID", example = "1")
    private Long reservationId;
    
    @Schema(description = "예매 번호", example = "R20260115-001")
    private String reservationNumber;
    
    @Schema(description = "예매일시", example = "2026-01-15T10:30:00")
    private LocalDateTime reservationDate;
    
    @Schema(description = "예매 상태", example = "CONFIRMED")
    private String reservationStatus;
    
    // ===== 공연 정보 =====
    @Schema(description = "공연 정보")
    private ShowInfo show;
    
    // ===== 일정 정보 =====
    @Schema(description = "일정 정보")
    private ScheduleInfo schedule;
    
    // ===== 좌석 정보 =====
    @Schema(description = "좌석 목록")
    private List<SeatInfo> seats;
    
    @Schema(description = "좌석 수", example = "2")
    private Integer seatCount;
    
    // ===== 예매자 정보 =====
    @Schema(description = "예매자 정보")
    private BookerInfo booker;
    
    // ===== 결제 정보 =====
    @Schema(description = "결제 정보")
    private PaymentInfo payment;
    
    // ===== 취소 관련 정보 =====
    @Schema(description = "취소 가능 여부", example = "true")
    private Boolean canCancel;
    
    @Schema(description = "취소일시", example = "2026-01-18T09:00:00")
    private LocalDateTime cancelledAt;
    
    /**
     * 공연 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "공연 정보")
    public static class ShowInfo {
        @Schema(description = "공연 ID", example = "1")
        private Long showId;
        
        @Schema(description = "공연 제목", example = "레미제라블")
        private String title;
        
        @Schema(description = "포스터 URL", example = "/images/posters/lesmiserables.jpg")
        private String posterUrl;
        
        @Schema(description = "장르", example = "MUSICAL")
        private String genre;
        
        @Schema(description = "상영 시간", example = "150분")
        private String runningTime;
        
        @Schema(description = "출연진", example = "김철수, 이영희")
        private String cast;
    }
    
    /**
     * 일정 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "일정 정보")
    public static class ScheduleInfo {
        @Schema(description = "스케줄 ID", example = "1")
        private Long scheduleId;
        
        @Schema(description = "공연일", example = "2026-02-20")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate showDate;
        
        @Schema(description = "공연 시간", example = "19:00")
        @JsonFormat(pattern = "HH:mm")
        private LocalTime showTime;
        
        @Schema(description = "공연 장소 정보")
        private LocationInfo location;
    }
    
    /**
     * 공연 장소 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "공연 장소 정보")
    public static class LocationInfo {
        @Schema(description = "지역", example = "SEOUL")
        private String region;
        
        @Schema(description = "공연장명", example = "예술의전당")
        private String venue;
        
        @Schema(description = "홀명", example = "오페라극장")
        private String hallName;
        
        @Schema(description = "주소", example = "서울특별시 서초구 남부순환로 2406")
        private String address;
    }
    
    /**
     * 좌석 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "좌석 정보")
    public static class SeatInfo {
        @Schema(description = "좌석 ID", example = "1")
        private Long seatId;
        
        @Schema(description = "구역명", example = "VIP석")
        private String section;
        
        @Schema(description = "행", example = "A")
        private String row;
        
        @Schema(description = "좌석 번호", example = "5")
        private Integer number;
        
        @Schema(description = "가격", example = "150000")
        private Integer price;
    }
    
    /**
     * 예매자 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "예매자 정보")
    public static class BookerInfo {
        @Schema(description = "이름", example = "홍길동")
        private String name;
        
        @Schema(description = "전화번호", example = "010-1234-5678")
        private String phone;
        
        @Schema(description = "이메일", example = "todayda1006@gmail.com")
        private String email;
    }
    
    /**
     * 결제 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "결제 정보")
    public static class PaymentInfo {
        @Schema(description = "주문 ID", example = "ORD20260115-001")
        private String orderId;
        
        @Schema(description = "결제 키", example = "tviva20260115abc123")
        private String paymentKey;
        
        @Schema(description = "총 결제 금액", example = "300000")
        private Integer totalAmount;
        
        @Schema(description = "결제 방법", example = "CARD")
        private String paymentMethod;
        
        @Schema(description = "결제 상태", example = "COMPLETED")
        private String paymentStatus;
        
        @Schema(description = "결제 승인일시", example = "2026-01-15T10:31:00")
        private LocalDateTime paidAt;
    }
}
