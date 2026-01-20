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

/**
 * 예매 목록 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "예매 목록 응답")
public class ReservationListResponse {
    
    @Schema(description = "예매 ID", example = "1")
    private Long reservationId;
    
    @Schema(description = "예매 번호", example = "R20260115-001")
    private String reservationNumber;
    
    @Schema(description = "예매일시", example = "2026-01-15T10:30:00")
    private LocalDateTime reservationDate;
    
    @Schema(description = "예매 상태", example = "CONFIRMED")
    private String reservationStatus;
    
    @Schema(description = "결제 상태", example = "COMPLETED")
    private String paymentStatus;
    
    @Schema(description = "공연 정보")
    private ShowInfo show;
    
    @Schema(description = "일정 정보")
    private ScheduleInfo schedule;
    
    @Schema(description = "좌석 수", example = "2")
    private Integer seatCount;
    
    @Schema(description = "총 결제 금액", example = "300000")
    private Integer totalAmount;
    
    @Schema(description = "취소 가능 여부", example = "true")
    private Boolean canCancel;
    
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
    }
}
