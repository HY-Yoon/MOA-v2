package com.moa2.api.admin.show.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "공연 일정 추가 요청")
public class ScheduleAddRequest {
    @Schema(description = "공연일", example = "2024-01-20", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "공연일은 필수입니다")
    private java.time.LocalDate showDate;

    @Schema(description = "공연 시간", example = "19:00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "공연 시간은 필수입니다")
    private String showTime; // "19:00" 형식

    @Schema(description = "티켓 오픈 시간", example = "2024-01-01T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "티켓 오픈 시간은 필수입니다")
    private LocalDateTime ticketOpenTime;
}

