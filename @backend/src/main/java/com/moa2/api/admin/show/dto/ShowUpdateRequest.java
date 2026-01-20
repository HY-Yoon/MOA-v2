package com.moa2.api.admin.show.dto;

import com.moa2.global.model.Region;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Schema(description = "공연 수정 요청")
public class ShowUpdateRequest {
    @Schema(description = "공연 제목", example = "레미제라블")
    private String title;

    @Schema(description = "장르 (ON_SALE 이후 수정 불가)", example = "MUSICAL")
    private String genre;

    @Schema(description = "장소 정보 (ON_SALE 이후 수정 불가)")
    @Valid
    private LocationRequest location;

    @Schema(description = "상영 시간", example = "150분")
    private String runningTime;

    @Schema(description = "출연진 정보 (문자열)", example = "출연진1, 출연진2, 출연진3")
    private String cast;

    @Schema(description = "판매 기간 (ON_SALE 이후 수정 불가)")
    @Valid
    private SalePeriodRequest salePeriod;

    @Schema(description = "공연 스케줄 목록 (추가/수정/삭제 모두 포함)\n\n" +
            "- **추가**: scheduleId 없음 (null)\n" +
            "- **수정**: scheduleId 있음\n" +
            "- **삭제**: deletedScheduleIds에 포함\n\n" +
            "팝업에서 변경한 모든 일정을 포함하여 '완료' 버튼 클릭 시 최종 저장됩니다.", example = "[{\"scheduleId\":1,\"showDate\":\"2026-01-15\",\"showTime\":\"19:00\",\"ticketOpenTime\":\"2026-01-01T10:00:00\"},{\"showDate\":\"2026-01-20\",\"showTime\":\"19:00\",\"ticketOpenTime\":\"2026-01-01T10:00:00\"}]")
    @Valid
    private List<ScheduleUpdateRequest> schedules;

    @Schema(description = "삭제할 스케줄 ID 목록", example = "[2, 3]")
    private List<Long> deletedScheduleIds;

    @Schema(description = "삭제할 상세 이미지 ID 목록 (수정 폼에서 기존 이미지 삭제 시 포함)", example = "[1, 3, 5]")
    private List<Long> deletedDetailImageIds;

    @Getter
    @Setter
    @Schema(description = "장소 정보")
    public static class LocationRequest {
        @Schema(description = "지역", example = "SEOUL")
        private Region region;

        @Schema(description = "공연장명", example = "올림픽공원")
        private String venueName;

        @Schema(description = "홀명", example = "KSPO DOME")
        private String hallName;
    }

    @Getter
    @Setter
    @Schema(description = "판매 기간")
    public static class SalePeriodRequest {
        @Schema(description = "판매 시작일시", example = "2026-01-01T10:00:00")
        private LocalDateTime startDate;

        @Schema(description = "판매 종료일시", example = "2026-01-31T23:59:59")
        private LocalDateTime endDate;
    }

    @Getter
    @Setter
    @Schema(description = "공연 스케줄 정보")
    public static class ScheduleUpdateRequest {
        @Schema(description = "스케줄 ID (수정 시 필수, 추가 시 null)", example = "1")
        private Long scheduleId;

        @Schema(description = "공연일", example = "2026-01-15", requiredMode = Schema.RequiredMode.REQUIRED)
        private java.time.LocalDate showDate;

        @Schema(description = "공연 시간", example = "19:00", requiredMode = Schema.RequiredMode.REQUIRED)
        private String showTime; // "19:00" 형식

        @Schema(description = "티켓 오픈 시간", example = "2026-01-01T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        private LocalDateTime ticketOpenTime;
    }
}
