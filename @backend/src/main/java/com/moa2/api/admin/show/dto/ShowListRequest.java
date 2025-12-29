package com.moa2.api.admin.show.dto;

import com.moa2.global.model.ShowStatus;
import com.moa2.global.model.SaleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
@Schema(description = "공연 목록 조회 요청")
public class ShowListRequest {
    @Schema(description = "공연 상태", example = "ON_SALE", allowableValues = {"WAITING", "ON_SALE", "SOLD_OUT", "ENDED", "SUSPENDED"})
    private ShowStatus showStatus;

    @Schema(description = "판매허용 상태", example = "ALLOWED", allowableValues = {"ALLOWED", "SUSPENDED"})
    private SaleStatus saleStatus;

    @Schema(description = "제목 검색 키워드", example = "뮤지컬")
    private String keyword;

    @Schema(description = "공연 시작일 검색", example = "2024-01-01")
    private LocalDate startDate;

    @Schema(description = "공연 종료일 검색", example = "2024-12-31")
    private LocalDate endDate;

    @Schema(description = "정렬 기준", example = "createdAt,desc", defaultValue = "createdAt,desc")
    private String sort = "createdAt,desc";

    @Schema(description = "페이지 번호", example = "0", defaultValue = "0")
    private int page = 0;

    @Schema(description = "페이지 크기", example = "20", defaultValue = "20")
    private int size = 20;
}

