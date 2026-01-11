package com.moa2.api.show.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 스케줄별 잔여석 조회 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "스케줄별 잔여석 조회 응답")
public class ScheduleSeatAvailabilityResponse {
    
    @Schema(description = "스케줄 ID", example = "1")
    private Long scheduleId;
    
    @Schema(description = "공연 ID", example = "1")
    private Long showId;
    
    @Schema(description = "공연일", example = "2025-02-20")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate showDate;
    
    @Schema(description = "공연 시간", example = "19:00")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime showTime;
    
    @Schema(description = "구역별 좌석 가용성 정보")
    private List<SeatAvailability> seatAvailability;
    
    @Schema(description = "전체 좌석 수", example = "2000")
    private Integer totalSeats;
    
    @Schema(description = "전체 잔여석 수", example = "1675")
    private Integer totalRemainingSeats;
    
    @Schema(description = "전체 가용률 (%)", example = "84")
    private Integer totalAvailabilityRate;
    
    /**
     * 구역별 좌석 가용성
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "구역별 좌석 가용성")
    public static class SeatAvailability {
        @Schema(description = "구역 ID", example = "1")
        private String sectionId;
        
        @Schema(description = "구역명", example = "VIP석")
        private String sectionName;
        
        @Schema(description = "가격", example = "150000")
        private Integer price;
        
        @Schema(description = "전체 좌석 수", example = "100")
        private Integer totalSeats;
        
        @Schema(description = "잔여석 수", example = "45")
        private Integer remainingSeats;
        
        @Schema(description = "가용률 (%)", example = "45")
        private Integer availabilityRate;
    }
}
