package com.moa2.api.admin.show.dto;

import com.moa2.global.model.SaleStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShowSaleStatusUpdateRequest {
    @NotNull(message = "판매 상태는 필수입니다")
    private SaleStatus saleStatus;
}

