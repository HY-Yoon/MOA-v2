package com.moa2.api.show.controller;

import com.moa2.api.show.controller.docs.ShowControllerDocs;
import com.moa2.api.show.dto.ShowDto;
import com.moa2.api.show.service.ShowService;
import com.moa2.global.dto.ApiResponse;
import com.moa2.global.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 사용자용 공연 조회 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/shows")
@RequiredArgsConstructor
public class ShowController implements ShowControllerDocs {

    private final ShowService showService;

    // 정렬 기준 상수
    private static final String SORT_LATEST = "createdAt";
    private static final String SORT_OLDEST = "oldest"; // createdAt ASC
    private static final String SORT_POPULARITY = "popularity"; // viewCount DESC
    private static final String SORT_TITLE = "title";
    private static final String SORT_START_DATE = "startDate";
    private static final String SORT_END_DATE = "endDate";

    /**
     * 공연 목록 조회
     */
    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ShowDto.ListResponse>>> getShowList(
            @ModelAttribute ShowDto.ListRequest request) {

        log.debug("공연 목록 조회 요청: genre={}, region={}, keyword={}", request.genre(), request.region(), request.keyword());

        // 2. Pageable 생성
        Pageable pageable = createPageable(request);

        // 3. 서비스 호출
        Page<ShowDto.ListResponse> pageResult = showService.getShowList(request, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(pageResult)));
    }

    private Pageable createPageable(ShowDto.ListRequest request) {
        int page = request.page() != null ? request.page() : 0;
        int size = request.size() != null ? request.size() : 20;

        // 기본 정렬: 최신순 (createdAt DESC)
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");

        String orderBy = request.orderBy();
        String orderDirection = request.orderDirection();

        if (orderBy != null && !orderBy.isBlank()) {
            Sort.Direction direction = "asc".equalsIgnoreCase(orderDirection) ? Sort.Direction.ASC
                    : Sort.Direction.DESC;

            switch (orderBy) {
                case SORT_POPULARITY:
                    sort = Sort.by(Sort.Direction.DESC, "viewCount");
                    break;
                case SORT_TITLE:
                    // 제목은 가나다순(ASC)이 기본이지만 사용자 요청 따름
                    sort = Sort.by(direction, "title");
                    break;
                case SORT_START_DATE:
                    sort = Sort.by(direction, "startDate");
                    break;
                case SORT_END_DATE:
                    sort = Sort.by(direction, "endDate");
                    break;
                case SORT_OLDEST:
                    sort = Sort.by(Sort.Direction.ASC, "createdAt");
                    break;
                case SORT_LATEST:
                default:
                    sort = Sort.by(Sort.Direction.DESC, "createdAt");
                    break;
            }
        }

        return PageRequest.of(page, size, sort);
    }

    /**
     * 공연 상세 조회
     */
    @Override
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShowDto.DetailResponse>> getShowDetail(@PathVariable Long id) {
        try {
                ShowDto.DetailResponse result = showService.getShowDetail(id);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (RuntimeException e) {
            log.error("공연 상세 조회 실패: showId={}, error={}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 날짜별 회차 조회
     */
    @Override
    @GetMapping("/{showId}/schedules")
    public ResponseEntity<ApiResponse<List<ShowDto.ScheduleListResponse>>> getShowSchedules(
            @PathVariable Long showId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        try {
            List<ShowDto.ScheduleListResponse> result = showService.getShowSchedules(showId, date);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (RuntimeException e) {
            log.error("공연 회차 조회 실패: showId={}, date={}, error={}", showId, date, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}