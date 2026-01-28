package com.moa2.api.show.controller;

import com.moa2.api.show.dto.*;
import com.moa2.api.show.service.ShowService;
import com.moa2.global.dto.ApiResponse;
import com.moa2.global.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 사용자용 공연 조회 컨트롤러
 */
@Slf4j
@Tag(name = "공연 조회 API", description = "사용자용 공연 조회 API")
@RestController
@RequestMapping("/api/v1/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    /**
     * 공연 목록 조회
     */
    @Operation(
            summary = "공연 목록 조회",
            description = "사용자가 예매 가능한 공연 목록을 조회합니다.\n\n" +
                    "**자동 필터링:**\n" +
                    "- 판매 허용(ALLOWED)된 공연만\n" +
                    "- 판매중(ON_SALE) 또는 매진(SOLD_OUT) 상태만\n\n" +
                    "**정렬 옵션:**\n" +
                    "- `createdAt`: 최신 등록순 (기본)\n" +
                    "- `startDate`: 공연 임박순\n" +
                    "- `viewCount`: 인기순\n" +
                    "- `title`: 가나다순"
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ShowDto.ListResponse>>> getShowList(
            @Parameter(description = "장르 필터")
            @RequestParam(required = false) String genre,
            @Parameter(description = "지역 필터")
            @RequestParam(required = false) String region,
            @Parameter(description = "제목 검색 (부분 일치)")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "공연 시작일 필터 (YYYY-MM-DD)")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "공연 종료일 필터 (YYYY-MM-DD)")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "정렬 기준 (createdAt, startDate, viewCount, title)")
            @RequestParam(defaultValue = "createdAt") String orderBy,
            @Parameter(description = "정렬 방향 (asc, desc)")
            @RequestParam(defaultValue = "desc") String orderDirection,
            @Parameter(description = "페이지 번호 (0부터 시작)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "20") int size) {

        log.debug("공연 목록 조회 요청 파라미터: genre={}, region={}, keyword={}, startDate={}, endDate={}",
                genre, region, keyword, startDate, endDate);

        // 1. 빌더 객체를 먼저 생성합니다.
        ShowDto.ListRequest.ListRequestBuilder requestBuilder = ShowDto.ListRequest.builder();

        // Genre enum 변환 및 세팅
        if (genre != null && !genre.trim().isEmpty()) {
            try {
                requestBuilder.genre(com.moa2.global.model.Genre.valueOf(genre.toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("유효하지 않은 장르: {}", genre);
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("유효하지 않은 장르입니다: " + genre));
            }
        }

        // Region enum 변환 및 세팅
        if (region != null && !region.trim().isEmpty()) {
            try {
                requestBuilder.region(com.moa2.global.model.Region.valueOf(region.toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("유효하지 않은 지역: {}", region);
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("유효하지 않은 지역입니다: " + region));
            }
        }

        // 나머지 필드들을 빌더에 담습니다. (메서드 체이닝 활용)
        requestBuilder
                .keyword((keyword != null && !keyword.trim().isEmpty()) ? keyword : null)
                .startDate(startDate)
                .endDate(endDate)
                .orderBy(orderBy)
                .orderDirection(orderDirection)
                .page(page)
                .size(size);

        // 2. 모든 재료가 모였을 때 최종적으로 객체를 '생성'합니다.
        ShowDto.ListRequest request = requestBuilder.build();

        // 이제 이 request 객체는 불변이며, 내부 값을 바꿀 수 없는 안전한 상태가 됩니다.

        // Sort 생성
        Sort.Direction direction = "desc".equalsIgnoreCase(orderDirection)
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, orderBy));

        Page<ShowDto.ListResponse> pageResult = showService.getShowList(request, pageable);
        PageResponse<ShowDto.ListResponse> result = PageResponse.of(pageResult);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 공연 상세 조회
     * ⭐ 조회 시 viewCount 자동 증가
     */
    @Operation(
            summary = "공연 상세 조회",
            description = "공연 상세 정보를 조회합니다.\n\n" +
                    "**주의:** 조회 시 해당 공연의 조회수(viewCount)가 자동으로 1 증가합니다."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShowDto.DetailResponse>> getShowDetail(
            @Parameter(description = "공연 ID", required = true) @PathVariable Long id) {

        try {
            ShowDto.DetailResponse result = showService.getShowDetail(id);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (RuntimeException e) {
            log.error("공연 상세 조회 실패: showId={}, error={}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

//    /**
//     * 스케줄별 잔여석 조회
//     */
//    @Operation(
//            summary = "스케줄별 잔여석 조회",
//            description = "특정 스케줄의 구역별 잔여석 정보를 조회합니다.\n\n" +
//                    "**응답 정보:**\n" +
//                    "- 구역별 전체 좌석 수, 잔여석 수, 가용률\n" +
//                    "- 전체 통계 (전체 좌석 수, 잔여석 수, 가용률)"
//    )
//    @GetMapping("/{id}/schedules/{scheduleId}/seats")
//    public ResponseEntity<ApiResponse<ShowDto.SeatAvailabilityResponse>> getScheduleSeatAvailability(
//            @Parameter(description = "공연 ID", required = true) @PathVariable Long id,
//            @Parameter(description = "스케줄 ID", required = true) @PathVariable Long scheduleId) {
//
//        try {
//            ShowDto.SeatAvailabilityResponse result = showService.getScheduleSeatAvailability(id, scheduleId);
//            return ResponseEntity.ok(ApiResponse.success(result));
//        } catch (RuntimeException e) {
//            log.error("스케줄별 잔여석 조회 실패: showId={}, scheduleId={}, error={}",
//                    id, scheduleId, e.getMessage());
//            return ResponseEntity.badRequest()
//                    .body(ApiResponse.error(e.getMessage()));
//        }
//    }

    /**
     * 날짜별 회차 조회
     */
    @Operation(
            summary = "날짜별 회차 조회",
            description = "특정 공연(showId)의 회차 목록을 조회합니다.\n\n" +
                    "- date 파라미터가 없으면 전체 회차 반환\n" +
                    "- date 파라미터가 있으면 해당 날짜의 회차만 반환\n" +
                    "- isSoldOut: 예약된 좌석 수 >= 전체 좌석 수 기준으로 계산"
    )
    @GetMapping("/{showId}/schedules")
    public ResponseEntity<ApiResponse<List<ShowDto.ScheduleListResponse>>> getShowSchedules(
            @Parameter(description = "공연 ID", required = true) @PathVariable Long showId,
            @Parameter(description = "공연 날짜 (YYYY-MM-DD, 선택)")
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
    ) {
        try {
            List<ShowDto.ScheduleListResponse> result = showService.getShowSchedules(showId, date);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (RuntimeException e) {
            log.error("공연 회차 조회 실패: showId={}, date={}, error={}", showId, date, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
