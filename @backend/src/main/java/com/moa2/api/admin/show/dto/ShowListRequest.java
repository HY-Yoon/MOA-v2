package com.moa2.api.admin.show.dto;

import com.moa2.global.model.ShowStatus;
import com.moa2.global.model.SaleStatus;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class ShowListRequest {
    private ShowStatus showStatus;
    private SaleStatus saleStatus;
    private String keyword;
    private LocalDate startDate;
    private LocalDate endDate;
    private String sort = "createdAt,desc";
    private int page = 0;
    private int size = 20;
}

