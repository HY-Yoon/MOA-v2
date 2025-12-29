package com.moa2.api.admin.show.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Schema(description = "공연 등록 요청")
public class ShowCreateRequest {
    @Schema(description = "공연 제목", example = "위키드", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "제목은 필수입니다")
    private String title;

    @Schema(description = "장르", example = "MUSICAL", allowableValues = {"MUSICAL", "CONCERT", "THEATER", "CLASSIC", "DANCE"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "장르는 필수입니다")
    private String genre;

    @Schema(description = "공연장 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "공연장 ID는 필수입니다")
    private Long venueId;

    @Schema(description = "상영 시간(분)", example = "150", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "상영 시간은 필수입니다")
    @Min(value = 1, message = "상영 시간은 1분 이상이어야 합니다")
    private Integer runningTime;

    @Schema(description = "포스터 URL (상대 경로)", example = "/images/posters/poster.jpg")
    private String posterUrl;

    @Schema(description = "상세 이미지 URL 목록 (상대 경로)", example = "[\"/images/details/detail1.jpg\", \"/images/details/detail2.jpg\"]")
    private String[] detailImageUrls;

    @Schema(description = "판매 시작일시", example = "2024-02-01T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "판매 시작일시는 필수입니다")
    private LocalDateTime saleStartDate;

    @Schema(description = "출연진 정보 (단순 문자열)", example = "김배우, 이배우, 박배우")
    private String cast;

    @Schema(description = "일정 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotEmpty(message = "스케줄은 최소 1개 이상 필요합니다")
    private List<ScheduleRequest> schedules;

    @Getter
    @Setter
    @Schema(description = "일정 요청")
    public static class ScheduleRequest {
        @Schema(description = "공연일", example = "2024-03-01", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "공연일은 필수입니다")
        private java.time.LocalDate showDate;

        @Schema(description = "공연 시간 (HH:mm 형식)", example = "19:00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "공연 시간은 필수입니다")
        private String showTime; // "19:00" 형식

        @Schema(description = "티켓 오픈 시간", example = "2024-02-01T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "티켓 오픈 시간은 필수입니다")
        private LocalDateTime ticketOpenTime;
    }
}

