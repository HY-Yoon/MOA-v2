package com.moa2.api.admin.show.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ShowCreateRequest {
    @NotBlank(message = "제목은 필수입니다")
    private String title;

    @NotNull(message = "장르는 필수입니다")
    private String genre;

    @NotBlank(message = "좌석배치도 ID는 필수입니다")
    private String seatMapId;

    @NotNull(message = "상영 시간은 필수입니다")
    @Min(value = 1, message = "상영 시간은 1분 이상이어야 합니다")
    private Integer runningTime;

    private String posterUrl;

    private String[] detailImageUrls;

    @NotNull(message = "판매 시작일시는 필수입니다")
    private LocalDateTime saleStartDate;

    @Valid
    @NotEmpty(message = "스케줄은 최소 1개 이상 필요합니다")
    private List<ScheduleRequest> schedules;

    @Valid
    private List<CastRequest> casts;

    @Valid
    private List<SeatPriceRequest> seatPrices;

    @Getter
    @Setter
    public static class ScheduleRequest {
        @NotNull(message = "공연일은 필수입니다")
        private java.time.LocalDate showDate;

        @NotNull(message = "공연 시간은 필수입니다")
        private String showTime; // "19:00" 형식

        @NotNull(message = "티켓 오픈 시간은 필수입니다")
        private LocalDateTime ticketOpenTime;
    }

    @Getter
    @Setter
    public static class CastRequest {
        @NotNull(message = "출연진 ID는 필수입니다")
        private Long castId;

        private String role;

        private List<Long> scheduleIds;
    }

    @Getter
    @Setter
    public static class SeatPriceRequest {
        @NotBlank(message = "구역 ID는 필수입니다")
        private String sectionId;

        @NotNull(message = "가격은 필수입니다")
        @Min(value = 0, message = "가격은 0원 이상이어야 합니다")
        private Integer price;
    }
}

