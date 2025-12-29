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
@Schema(description = "공연 삭제 응답")
public class ShowDeleteResponse {
    @Schema(description = "삭제 완료 메시지", example = "공연이 성공적으로 삭제되었습니다", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;
}

