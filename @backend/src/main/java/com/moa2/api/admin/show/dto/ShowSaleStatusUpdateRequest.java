package com.moa2.api.admin.show.dto;

import com.moa2.global.model.SaleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "공연 판매 상태 변경 요청")
public class ShowSaleStatusUpdateRequest {
    @Schema(description = "판매 상태", example = "ALLOWED", allowableValues = {"ALLOWED", "SUSPENDED"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "판매 상태는 필수입니다")
    private SaleStatus saleStatus;
}

