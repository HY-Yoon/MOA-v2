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
@Schema(description = "공연 수정 응답")
public class ShowUpdateResponse {
    @Schema(description = "수정된 공연 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long showId;

    @Schema(description = "응답 메시지", example = "공연 정보가 성공적으로 수정되었습니다", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;
}

