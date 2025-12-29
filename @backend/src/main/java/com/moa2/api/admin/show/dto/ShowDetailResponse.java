package com.moa2.api.admin.show.dto;

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
public class ShowDetailResponse {
    private Long id;
    private String title;
    private String genre;
    private String seatMapId;
    private String venueName;
    private String hallName;
    private String region;
    private Integer runningTime;
    private String posterUrl;
    private String[] detailImageUrls;
    private String status;
    private String saleStatus;
    private LocalDateTime saleStartDate;
    private LocalDateTime saleEndDate;
    private List<ScheduleInfo> schedules;
    private List<CastInfo> casts;
    private List<SeatPriceInfo> seatPrices;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleInfo {
        private Long scheduleId;
        private LocalDate showDate;
        private LocalTime showTime;
        private LocalDateTime ticketOpenTime;
        private Integer remainingSeats;
        private Integer totalSeats;
        private Integer reservationCount;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CastInfo {
        private Long castId;
        private String name;
        private String role;
        private List<Long> scheduleIds;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatPriceInfo {
        private String sectionId;
        private String sectionName;
        private Integer price;
    }
}

