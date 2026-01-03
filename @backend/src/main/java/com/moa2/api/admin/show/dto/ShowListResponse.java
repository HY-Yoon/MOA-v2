package com.moa2.api.admin.show.dto;

import com.moa2.global.model.ShowStatus;
import com.moa2.global.model.SaleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowListResponse {
    private Long id;
    private String title;
    private String genre;
    private String status;
    private String saleStatus;
    private String venue;
    private String region;
    private String hallName;
    private LocalDateTime firstScheduleDate;
    private LocalDateTime saleStartDate;
    private LocalDateTime saleEndDate;
}

