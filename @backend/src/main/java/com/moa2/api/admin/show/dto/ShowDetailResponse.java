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

    @Schema(description = "공연 제목", example = "레미제라블", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "장르", example = "MUSICAL", allowableValues = {"MUSICAL", "CONCERT", "PLAY", "CLASSIC", "DANCE"})
    private String genre;

    @Schema(description = "시설명", example = "예술의전당")
    private String venueName;

    @Schema(description = "홀명", example = "오페라극장")
    private String hallName;

    @Schema(description = "지역", example = "SEOUL", allowableValues = {"SEOUL", "GYEONGGI", "INCHEON", "BUSAN", "DAEGU", "DAEJEON", "GWANGJU", "ULSAN", "SEJONG", "GANGWON", "CHUNGBUK", "CHUNGNAM", "JEONBUK", "JEONNAM", "GYEONGBUK", "GYEONGNAM", "JEJU"})
    private String region;

    @Schema(description = "상영 시간(분)", example = "150")
    private Integer runningTime;

    @Schema(description = "포스터 이미지 URL", example = "/images/posters/show1.jpg")
    private String posterUrl;

    @Schema(description = "상세 이미지 URL 배열", example = "[\"/images/details/show1-1.jpg\", \"/images/details/show1-2.jpg\"]")
    private String[] detailImageUrls;

    @Schema(description = "출연진 정보", example = "김철수, 이영희, 박민수")
    private String cast;

    @Schema(description = "공연 상태", example = "ON_SALE", allowableValues = {"WAITING", "ON_SALE", "SOLD_OUT", "ENDED", "SUSPENDED"})
    private String status;

    @Schema(description = "판매 상태", example = "ALLOWED", allowableValues = {"ALLOWED", "NOT_ALLOWED"})
    private String saleStatus;

    @Schema(description = "판매 시작일시", example = "2024-01-01T00:00:00")
    private LocalDateTime saleStartDate;

    @Schema(description = "판매 종료일시", example = "2024-01-31T23:59:59")
    private LocalDateTime saleEndDate;

    @Schema(description = "공연 스케줄 목록")
    private List<ScheduleInfo> schedules;

    @Schema(description = "좌석 가격 정보 목록")
    private List<SeatPriceInfo> seatPrices;

    @Schema(description = "생성일시", example = "2024-01-01T00:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2024-01-01T00:00:00")
    private LocalDateTime updatedAt;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "공연 스케줄 정보")
    public static class ScheduleInfo {
        @Schema(description = "스케줄 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long scheduleId;

        @Schema(description = "공연일", example = "2024-01-15", requiredMode = Schema.RequiredMode.REQUIRED)
        private LocalDate showDate;

        @Schema(description = "공연 시간", example = "19:00", requiredMode = Schema.RequiredMode.REQUIRED)
        private LocalTime showTime;

        @Schema(description = "티켓 오픈 시간", example = "2024-01-01T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        private LocalDateTime ticketOpenTime;

        @Schema(description = "남은 좌석 수", example = "50", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer remainingSeats;

        @Schema(description = "전체 좌석 수", example = "2000", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer totalSeats;

        @Schema(description = "예매 수", example = "1950", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer reservationCount;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "좌석 가격 정보")
    public static class SeatPriceInfo {
        @Schema(description = "구역 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private String sectionId;

        @Schema(description = "구역명", example = "VIP석", requiredMode = Schema.RequiredMode.REQUIRED)
        private String sectionName;

        @Schema(description = "가격", example = "150000", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer price;
    }
}

