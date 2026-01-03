package com.moa2.api.admin.seatmap.controller;

import com.moa2.api.admin.seatmap.dto.*;
import com.moa2.api.admin.seatmap.service.AdminSeatMapService;
import com.moa2.global.dto.ApiResponse;
import com.moa2.global.dto.PageResponse;
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
    
    @Operation(
        summary = "좌석배치도 목록 조회",
        description = "필터링 조건에 맞는 좌석배치도 목록을 조회합니다.\n\n" +
                     "**필터링 파라미터 (모두 optional):**\n" +
                     "- `region`: 지역명 (예: \"서울\", \"경기\")\n" +
                     "- `venueName`: 공연장명\n" +
                     "- `hallName`: 홀명\n\n" +
                     "**페이징 파라미터:**\n" +
                     "- `page`: 페이지 번호 (0부터 시작, 기본값: 0)\n" +
                     "- `size`: 페이지 크기 (기본값: 20)"
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SeatMapListResponse>>> getSeatMapList(
            @Parameter(description = "지역명 (예: 서울, 경기)", example = "서울")
            @RequestParam(required = false) String region,
            @Parameter(description = "공연장명", example = "올림픽공원")
            @RequestParam(required = false) String venueName,
            @Parameter(description = "홀명", example = "KSPO DOME")
            @RequestParam(required = false) String hallName,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        
        SeatMapListRequest request = new SeatMapListRequest();
        request.setRegion(region);
        request.setVenueName(venueName);
        request.setHallName(hallName);
        request.setPage(page);
        request.setSize(size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SeatMapListResponse> pageResult = adminSeatMapService.getSeatMapList(request, pageable);
        PageResponse<SeatMapListResponse> result = PageResponse.of(pageResult);
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    @Operation(
        summary = "좌석배치도 중복 검사",
        description = "지역, 공연장명, 홀명으로 좌석배치도 중복 여부를 확인합니다.\n\n" +
                     "**파라미터 (모두 optional, 하지만 모두 제공되어야 정확한 검사 가능):**\n" +
                     "- `region`: 지역명 (예: \"서울\", \"경기\")\n" +
                     "- `venueName`: 공연장명\n" +
                     "- `hallName`: 홀명\n\n" +
                     "**응답:**\n" +
                     "- `isDuplicate`: 중복 여부 (true: 중복됨, false: 중복되지 않음)"
    )
    @GetMapping("/duplicate")
    public ResponseEntity<ApiResponse<SeatMapDuplicateCheckResponse>> checkDuplicate(
            @Parameter(description = "지역명 (예: 서울, 경기)", example = "서울")
            @RequestParam(required = false) String region,
            @Parameter(description = "공연장명", example = "올림픽공원")
            @RequestParam(required = false) String venueName,
            @Parameter(description = "홀명", example = "KSPO DOME")
            @RequestParam(required = false) String hallName) {
        
        SeatMapDuplicateCheckRequest request = new SeatMapDuplicateCheckRequest();
        request.setRegion(region);
        request.setVenueName(venueName);
        request.setHallName(hallName);
        
        SeatMapDuplicateCheckResponse result = adminSeatMapService.checkDuplicate(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    @Operation(
        summary = "좌석배치도 등록",
        description = "새로운 좌석배치도를 등록합니다.\n\n" +
                     "**필수 필드:**\n" +
                     "- `region`: 지역명 (예: \"서울\", \"경기\")\n" +
                     "- `venueName`: 공연장명\n" +
                     "- `hallName`: 홀명\n" +
                     "- `canvas`: 캔버스 정보 (width, height, seatRadius)\n" +
                     "- `sections`: 구역 목록\n" +
                     "- `seats`: 좌석 목록\n\n" +
                     "**중복 검사:**\n" +
                     "- region + venueName + hallName 조합이 이미 존재하면 등록 불가"
    )
    @PostMapping
    public ResponseEntity<ApiResponse<SeatMapCreateResponse>> createSeatMap(
            @Valid @RequestBody SeatMapCreateRequest request) {
        
        SeatMapCreateResponse result = adminSeatMapService.createSeatMap(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(result));
    }
}

