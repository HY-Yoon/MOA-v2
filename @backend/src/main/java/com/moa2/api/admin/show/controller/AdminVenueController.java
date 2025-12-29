package com.moa2.api.admin.show.controller;

import com.moa2.api.admin.show.dto.VenueCreateRequest;
import com.moa2.api.admin.show.dto.VenueCreateResponse;
import com.moa2.api.admin.show.dto.VenueListResponse;
import com.moa2.api.admin.show.service.AdminVenueService;
import com.moa2.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "장소 관리 API", description = "관리자용 장소 관리 API")
@RestController
@RequestMapping("/api/v1/admin/venues")
@RequiredArgsConstructor
public class AdminVenueController {

    private final AdminVenueService adminVenueService;

    @Operation(summary = "장소 목록 조회", description = "공연 등록 시 Select Box에서 사용할 전체 장소 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<VenueListResponse>>> getVenueList() {
        List<VenueListResponse> result = adminVenueService.getVenueList();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "장소 등록", description = "새로운 장소 정보를 등록합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<VenueCreateResponse>> createVenue(
            @Valid @RequestBody VenueCreateRequest request) {
        VenueCreateResponse result = adminVenueService.createVenue(request);
        return ResponseEntity.status(HttpStatus.OK)
            .body(ApiResponse.success(result, result.getMessage()));
    }
}

