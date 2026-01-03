package com.moa2.api.admin.show.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moa2.global.model.SaleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "판매 상태 변경 요청")
public class ShowSaleStatusUpdateRequest {

    @JsonProperty("saleStatus")
    @Schema(
        description = "판매 상태 (필수 필드명: saleStatus)", 
        example = "SUSPENDED", 
        requiredMode = Schema.RequiredMode.REQUIRED, 
        allowableValues = {"ALLOWED", "SUSPENDED"}
    )
    @NotNull(message = "판매 상태는 필수입니다")
    private SaleStatus saleStatus;
}

