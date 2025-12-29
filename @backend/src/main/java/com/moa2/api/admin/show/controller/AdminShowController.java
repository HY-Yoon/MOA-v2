package com.moa2.api.admin.show.controller;

import com.moa2.api.admin.show.dto.*;
import com.moa2.api.admin.show.service.AdminShowService;
import com.moa2.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "공연 관리 API", description = "관리자용 공연 관리 API")
@RestController
@RequestMapping("/api/v1/admin/shows")
@RequiredArgsConstructor
public class AdminShowController {

    private final AdminShowService adminShowService;

    @Operation(summary = "공연 목록 조회", description = "관리자용 공연 목록을 조회합니다. 필터링, 검색, 페이징을 지원합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ShowListResponse>>> getShowList(
            @RequestParam(required = false) String showStatus,
            @RequestParam(required = false) String saleStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) java.time.LocalDate startDate,
            @RequestParam(required = false) java.time.LocalDate endDate,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        ShowListRequest request = new ShowListRequest();
        if (showStatus != null) {
            request.setShowStatus(com.moa2.global.model.ShowStatus.valueOf(showStatus));
        }
        if (saleStatus != null) {
            request.setSaleStatus(com.moa2.global.model.SaleStatus.valueOf(saleStatus));
        }
        request.setKeyword(keyword);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setSort(sort);
        request.setPage(page);
        request.setSize(size);

        // Sort 파싱
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && "desc".equalsIgnoreCase(sortParts[1])
            ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        Page<ShowListResponse> result = adminShowService.getShowList(request, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "공연 상세 조회", description = "관리자용 공연 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShowDetailResponse>> getShowDetail(
            @Parameter(description = "공연 ID", required = true) @PathVariable Long id) {
        ShowDetailResponse result = adminShowService.getShowDetail(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "공연 등록", description = "새로운 공연을 등록합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<ShowCreateResponse>> createShow(
            @Valid @RequestBody ShowCreateRequest request) {
        ShowCreateResponse result = adminShowService.createShow(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(result, result.getMessage()));
    }

    @Operation(summary = "공연 수정", description = "공연 정보를 수정합니다. WAITING 상태에서는 전체 수정 가능, ON_SALE 이후에는 제한된 필드만 수정 가능합니다.")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ShowUpdateResponse>> updateShow(
            @Parameter(description = "공연 ID", required = true) @PathVariable Long id,
            @Valid @RequestBody ShowUpdateRequest request) {
        ShowUpdateResponse result = adminShowService.updateShow(id, request);
        return ResponseEntity.ok(ApiResponse.success(result, result.getMessage()));
    }

    @Operation(summary = "공연 삭제", description = "공연을 삭제합니다. WAITING 상태의 공연만 삭제 가능합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<ShowDeleteResponse>> deleteShow(
            @Parameter(description = "공연 ID", required = true) @PathVariable Long id) {
        ShowDeleteResponse result = adminShowService.deleteShow(id);
        return ResponseEntity.ok(ApiResponse.success(result, result.getMessage()));
    }

    @Operation(summary = "공연 판매 상태 변경", description = "공연의 판매 상태를 변경합니다.")
    @PatchMapping("/{id}/sale-status")
    public ResponseEntity<ApiResponse<ShowSaleStatusUpdateResponse>> updateSaleStatus(
            @Parameter(description = "공연 ID", required = true) @PathVariable Long id,
            @Valid @RequestBody ShowSaleStatusUpdateRequest request) {
        ShowSaleStatusUpdateResponse result = adminShowService.updateSaleStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(result, result.getMessage()));
    }
}

