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
@Schema(description = "장소 목록 조회 응답")
public class VenueListResponse {
    @Schema(description = "장소 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "시설명", example = "예술의전당", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "홀명", example = "오페라극장", requiredMode = Schema.RequiredMode.REQUIRED)
    private String hallName;
}

