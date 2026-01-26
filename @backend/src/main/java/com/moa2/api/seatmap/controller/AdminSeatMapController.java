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

    @Operation(summary = "좌석배치도 목록 조회", description = "필터링 조건에 맞는 좌석배치도 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SeatmapDto.ListResponse>>> getSeatMapList(
            @Parameter(description = "지역", example = "SEOUL") @RequestParam(required = false) Region region,
            @Parameter(description = "공연장명", example = "올림픽공원") @RequestParam(required = false) String venueName,
            @Parameter(description = "홀명", example = "KSPO DOME") @RequestParam(required = false) String hallName,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20") @RequestParam(defaultValue = "20") int size) {

        // ✅ Record 빌더를 사용하여 객체 생성
        SeatmapDto.ListRequest request = SeatmapDto.ListRequest.builder()
                .region(region)
                .venueName(venueName)
                .hallName(hallName)
                .page(page)
                .size(size)
                .build();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SeatmapDto.ListResponse> pageResult = adminSeatMapService.getSeatMapList(request, pageable);
        PageResponse<SeatmapDto.ListResponse> result = PageResponse.of(pageResult);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "좌석배치도 중복 검사", description = "지역, 공연장명, 홀명으로 좌석배치도 중복 여부를 확인합니다.")
    @GetMapping("/duplicate")
    public ResponseEntity<ApiResponse<SeatmapDto.DuplicateCheckResponse>> checkDuplicate(
            @Parameter(description = "지역", example = "SEOUL") @RequestParam(required = false) Region region,
            @Parameter(description = "공연장명", example = "올림픽공원") @RequestParam(required = false) String venueName,
            @Parameter(description = "홀명", example = "KSPO DOME") @RequestParam(required = false) String hallName) {

        // ✅ Record 빌더를 사용하여 객체 생성
        SeatmapDto.DuplicateCheckRequest request = SeatmapDto.DuplicateCheckRequest.builder()
                .region(region)
                .venueName(venueName)
                .hallName(hallName)
                .build();

        SeatmapDto.DuplicateCheckResponse result = adminSeatMapService.checkDuplicate(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "좌석배치도 등록", description = "새로운 좌석배치도를 등록합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<SeatmapDto.CreateResponse>> createSeatMap(
            @Valid @RequestBody SeatmapDto.CreateRequest request) {

        // @RequestBody를 통해 들어오는 JSON은 Jackson이 자동으로 Record를 생성해주므로 별도 빌더 작업 불필요
        SeatmapDto.CreateResponse result = adminSeatMapService.createSeatMap(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result));
    }
}