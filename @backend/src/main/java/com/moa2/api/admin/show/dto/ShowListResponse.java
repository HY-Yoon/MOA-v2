package com.moa2.api.admin.show.dto;

import com.moa2.global.model.ShowStatus;
import com.moa2.global.model.SaleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "공연 목록 조회 응답")
public class ShowListResponse {
    @Schema(description = "공연 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "공연 제목", example = "위키드", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "장르", example = "MUSICAL", requiredMode = Schema.RequiredMode.REQUIRED)
    private String genre;

    @Schema(description = "공연 상태", example = "ON_SALE", allowableValues = {"WAITING", "ON_SALE", "SOLD_OUT", "ENDED", "SUSPENDED"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    @Schema(description = "판매허용 상태", example = "ALLOWED", allowableValues = {"ALLOWED", "SUSPENDED"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private String saleStatus;

    @Schema(description = "장소", example = "세종문화회관", requiredMode = Schema.RequiredMode.REQUIRED)
    private String venue;

    @Schema(description = "지역", example = "SEOUL", requiredMode = Schema.RequiredMode.REQUIRED)
    private String region;

    @Schema(description = "첫 공연일", example = "2024-03-01T19:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime firstScheduleDate;

    @Schema(description = "예매 시작일", example = "2024-02-01T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime saleStartDate;

    @Schema(description = "예매 종료일", example = "2024-03-31T23:59:59", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime saleEndDate;
}

