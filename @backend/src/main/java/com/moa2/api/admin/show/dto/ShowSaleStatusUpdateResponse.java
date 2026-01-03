package com.moa2.api.admin.show.dto;

import com.moa2.global.model.SaleStatus;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ShowSaleStatusUpdateResponse {
    private Long showId;
    private SaleStatus saleStatus;
    private String message;
}

