package com.moa2.api.admin.show.dto;

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
@Schema(description = "공연 상세 조회 응답")
public class ShowDetailResponse {
    @Schema(description = "공연 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "공연 제목", example = "위키드", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "장르", example = "MUSICAL", requiredMode = Schema.RequiredMode.REQUIRED)
    private String genre;

    @Schema(description = "장소명", example = "예술의전당", requiredMode = Schema.RequiredMode.REQUIRED)
    private String venueName;

    @Schema(description = "홀명", example = "대극장", requiredMode = Schema.RequiredMode.REQUIRED)
    private String hallName;

    @Schema(description = "지역", example = "SEOUL", requiredMode = Schema.RequiredMode.REQUIRED)
    private String region;

    @Schema(description = "상영 시간(분)", example = "150", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer runningTime;

    @Schema(description = "포스터 URL (상대 경로)", example = "/images/posters/poster.jpg")
    private String posterUrl;

    @Schema(description = "상세 이미지 URL 목록 (상대 경로)", example = "[\"/images/details/detail1.jpg\", \"/images/details/detail2.jpg\"]")
    private String[] detailImageUrls;

    @Schema(description = "공연 상태", example = "ON_SALE", allowableValues = {"WAITING", "ON_SALE", "SOLD_OUT", "ENDED", "SUSPENDED"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    @Schema(description = "판매허용 상태", example = "ALLOWED", allowableValues = {"ALLOWED", "SUSPENDED"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private String saleStatus;

    @Schema(description = "예매 시작일시", example = "2024-02-01T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime saleStartDate;

    @Schema(description = "예매 종료일시", example = "2024-03-31T23:59:59", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime saleEndDate;

    @Schema(description = "출연진 정보 (단순 문자열)", example = "김배우, 이배우, 박배우")
    private String cast;

    @Schema(description = "일정 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ScheduleInfo> schedules;

    @Schema(description = "좌석 가격 정보", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<SeatPriceInfo> seatPrices;

    @Schema(description = "생성일시", example = "2024-01-15T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2024-01-20T15:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updatedAt;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "일정 정보")
    public static class ScheduleInfo {
        @Schema(description = "일정 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long scheduleId;

        @Schema(description = "공연일", example = "2024-03-01", requiredMode = Schema.RequiredMode.REQUIRED)
        private LocalDate showDate;

        @Schema(description = "공연 시간", example = "19:00", requiredMode = Schema.RequiredMode.REQUIRED)
        private LocalTime showTime;

        @Schema(description = "티켓 오픈 시간", example = "2024-02-01T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        private LocalDateTime ticketOpenTime;

        @Schema(description = "잔여석", example = "50", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer remainingSeats;

        @Schema(description = "전체 좌석 수", example = "200", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer totalSeats;

        @Schema(description = "예매 수", example = "150", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer reservationCount;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "좌석 가격 정보")
    public static class SeatPriceInfo {
        @Schema(description = "구역 ID", example = "section-001", requiredMode = Schema.RequiredMode.REQUIRED)
        private String sectionId;

        @Schema(description = "구역명", example = "VIP석", requiredMode = Schema.RequiredMode.REQUIRED)
        private String sectionName;

        @Schema(description = "가격", example = "150000", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer price;
    }
}

