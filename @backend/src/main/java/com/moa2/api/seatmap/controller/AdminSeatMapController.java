package com.moa2.api.seatmap.controller;

import com.moa2.api.seatmap.dto.*;
import com.moa2.api.seatmap.service.AdminSeatMapService;
import com.moa2.global.dto.ApiResponse;
import com.moa2.global.dto.PageResponse;
import com.moa2.global.model.Region;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "좌석배치도 관리 API", description = "관리자용 좌석배치도 관리 API")
@RestController
@RequestMapping("/api/v1/admin/seat-maps")
@RequiredArgsConstructor
public class AdminSeatMapController {

    private final AdminSeatMapService adminSeatMapService;
    private static final String SORT_CREATED_AT = "createdAt";

    @Operation(summary = "좌석배치도 목록 조회", description = "필터링 조건에 맞는 좌석배치도 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SeatmapDto.ListResponse>>> getSeatMapList(
            @ParameterObject @ModelAttribute SeatmapDto.ListRequest request) {

        Pageable pageable = PageRequest.of(
                request.page(),
                request.size(),
                Sort.by(Sort.Direction.DESC, SORT_CREATED_AT)
        );

        Page<SeatmapDto.ListResponse> pageResult = adminSeatMapService.getSeatMapList(request, pageable);
        PageResponse<SeatmapDto.ListResponse> result = PageResponse.of(pageResult);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "좌석배치도 중복 검사", description = "지역, 공연장명, 홀명으로 좌석배치도 중복 여부를 확인합니다.")
    @GetMapping("/duplicate")
    public ResponseEntity<ApiResponse<SeatmapDto.DuplicateCheckResponse>> checkDuplicate(
            @ParameterObject @ModelAttribute @Valid SeatmapDto.DuplicateCheckRequest request) {

        SeatmapDto.DuplicateCheckResponse result = adminSeatMapService.checkDuplicate(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "좌석배치도 등록", description = "새로운 좌석배치도를 등록합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<SeatmapDto.CreateResponse>> createSeatMap(
            @Valid @RequestBody SeatmapDto.CreateRequest request) {

        SeatmapDto.CreateResponse result = adminSeatMapService.createSeatMap(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result));
    }
}