package com.moa2.api.admin.show.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "장소 등록 응답")
public class VenueCreateResponse {
    @Schema(description = "생성된 장소 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long venueId;

    @Schema(description = "응답 메시지", example = "장소가 성공적으로 등록되었습니다", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;
}

