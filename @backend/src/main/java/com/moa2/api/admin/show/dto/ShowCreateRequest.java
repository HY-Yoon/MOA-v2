package com.moa2.api.admin.show.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Schema(description = "공연 등록 요청")
public class ShowCreateRequest {
    @Schema(description = "공연 제목", example = "레미제라블", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "제목은 필수입니다")
    private String title;

    @Schema(description = "장르", example = "MUSICAL", allowableValues = {"MUSICAL", "CONCERT", "PLAY", "CLASSIC", "DANCE"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "장르는 필수입니다")
    private String genre;

    @Schema(description = "장소 정보", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull(message = "장소 정보는 필수입니다")
    private LocationRequest location;

    @Schema(description = "상영 시간(분)", example = "150", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "상영 시간은 필수입니다")
    @Min(value = 1, message = "상영 시간은 1분 이상이어야 합니다")
    private Integer runningTime;

    @Schema(description = "출연진 정보 (문자열)", example = "김철수, 이영희, 박민수")
    private String cast;

    @Schema(description = "예약 가능 기간", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull(message = "예약 가능 기간은 필수입니다")
    private BookingPeriodRequest bookingPeriod;

    @Schema(description = "공연 스케줄 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotEmpty(message = "스케줄은 최소 1개 이상 필요합니다")
    private List<ScheduleRequest> schedules;

    @Getter
    @Setter
    @Schema(description = "장소 정보")
    public static class LocationRequest {
        @Schema(description = "지역", example = "서울", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "지역은 필수입니다")
        private String region;

        @Schema(description = "공연장명", example = "올림픽공원", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "공연장명은 필수입니다")
        private String venueName;

        @Schema(description = "홀명", example = "KSPO DOME", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "홀명은 필수입니다")
        private String hallName;
    }

    @Getter
    @Setter
    @Schema(description = "예약 가능 기간")
    public static class BookingPeriodRequest {
        @Schema(description = "예약 시작일", example = "2024-01-01", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "예약 시작일은 필수입니다")
        private LocalDate startDate;

        @Schema(description = "예약 종료일", example = "2024-01-31", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "예약 종료일은 필수입니다")
        private LocalDate endDate;
    }

    @Getter
    @Setter
    @Schema(description = "공연 스케줄 정보")
    public static class ScheduleRequest {
        @Schema(description = "공연일", example = "2024-01-15", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "공연일은 필수입니다")
        private java.time.LocalDate showDate;

        @Schema(description = "공연 시간", example = "19:00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "공연 시간은 필수입니다")
        private String showTime; // "19:00" 형식

        @Schema(description = "티켓 오픈 시간", example = "2024-01-01T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "티켓 오픈 시간은 필수입니다")
        private java.time.LocalDateTime ticketOpenTime;
    }
}

