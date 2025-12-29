package com.moa2.api.admin.show.dto;

import com.moa2.global.model.SaleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "공연 판매 상태 변경 응답")
public class ShowSaleStatusUpdateResponse {
    @Schema(description = "공연 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long showId;

    @Schema(description = "업데이트된 판매 상태", example = "ALLOWED", allowableValues = {"ALLOWED", "SUSPENDED"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private SaleStatus saleStatus;

    @Schema(description = "응답 메시지", example = "판매 상태가 성공적으로 변경되었습니다", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;
}

