package com.moa2.api.admin.show.dto;

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

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowListResponse {
    private Long id;
    private String title;
    private String genre;
    private String status;
    private String saleStatus;
    private String venue;
    private String region;
    private String hallName;
    private LocalDateTime firstScheduleDate;
    
    // 판매 기간 (saleStartDate, saleEndDate를 하나로 통합)
    private SalePeriod salePeriod;
    
    // 일정 목록
    private List<ScheduleInfo> schedules;
    
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
        
        @Schema(description = "공연일", example = "2024-01-20", type = "string")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        
        @Schema(description = "공연 시간", example = "19:00", type = "string")
        @JsonFormat(pattern = "HH:mm")
        private LocalTime time;
        
        @Schema(description = "회차", example = "1")
        private Integer session;
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
        @Schema(description = "판매 시작일시", example = "2024-01-01T10:00:00", type = "string")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime startDate;
        
        @Schema(description = "판매 종료일시", example = "2024-01-31T23:59:59", type = "string")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime endDate;
    }
}

