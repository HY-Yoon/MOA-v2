package com.moa2.api.show.dto;

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
 * 사용자용 공연 목록 조회 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "공연 목록 조회 응답")
public class ShowListResponse {
    
    @Schema(description = "공연 ID", example = "1")
    private Long id;
    
    @Schema(description = "공연 제목", example = "레미제라블")
    private String title;
    
    @Schema(description = "장르", example = "MUSICAL")
    private String genre;
    
    @Schema(description = "공연 상태", example = "ON_SALE")
    private String status;
    
    @Schema(description = "포스터 URL", example = "/images/posters/show1.jpg")
    private String posterUrl;
    
    @Schema(description = "공연 장소 정보")
    private LocationInfo location;
    
    @Schema(description = "판매 기간")
    private SalePeriod salePeriod;
    
    @Schema(description = "공연 일정 목록")
    private List<ScheduleInfo> schedules;
    
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
    
    /**
     * 판매 기간 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "판매 기간 정보")
    public static class SalePeriod {
        @Schema(description = "판매 시작일시", example = "2025-01-01T10:00:00")
        private LocalDateTime startDate;
        
        @Schema(description = "판매 종료일시", example = "2025-12-31T23:59:59")
        private LocalDateTime endDate;
    }
    
    /**
     * 공연 일정 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "공연 일정 정보")
    public static class ScheduleInfo {
        @Schema(description = "일정 ID", example = "1")
        private Long keyId;
        
        @Schema(description = "공연일", example = "2025-02-20")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        
        @Schema(description = "공연 시간", example = "19:00")
        @JsonFormat(pattern = "HH:mm")
        private LocalTime time;
        
        @Schema(description = "회차", example = "1")
        private Integer session;
    }
}
