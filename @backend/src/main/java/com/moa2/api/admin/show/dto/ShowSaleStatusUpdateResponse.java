package com.moa2.api.admin.show.dto;

import com.moa2.global.model.SaleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowSaleStatusUpdateResponse {
    private Long showId;
    private SaleStatus saleStatus;
    private String message;
}

