package com.moa2.api.reservation.controller;

import com.moa2.api.reservation.controller.docs.AdminReservationControllerDocs;
import com.moa2.api.reservation.dto.AdminReservationDto;
import com.moa2.api.reservation.service.AdminReservationService;
import com.moa2.global.dto.ApiResponse;
import com.moa2.global.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/reservations")
@RequiredArgsConstructor
public class AdminReservationController implements AdminReservationControllerDocs {

    private final AdminReservationService adminReservationService;
    private static final String SORT_CREATED_AT = "createdAt";

    /**
     * 예매 내역 조회
     * 
     * @param status
     * @param page
     * @param size
     * @return
     */
    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminReservationDto.ListResponse>>> getReservations(
            @ParameterObject @ModelAttribute AdminReservationDto.SearchCondition condition,
            @RequestParam(value = "dateType", required = false) String dateType,
            @RequestParam(value = "dateSearchType", required = false) String dateSearchType) {

        AdminReservationDto.SearchCondition normalizedCondition = normalizeCondition(condition, dateType, dateSearchType);

        // 최신순 정렬 (Service에서 쿼리 레벨로 정렬하지만, Pageable 객체 전달용)
        Pageable pageable = PageRequest.of(
                normalizedCondition.page(),
                normalizedCondition.size(),
                Sort.by(Sort.Direction.DESC, SORT_CREATED_AT));

        PageResponse<AdminReservationDto.ListResponse> response = adminReservationService.getReservations(
                normalizedCondition,
                pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private AdminReservationDto.SearchCondition normalizeCondition(
            AdminReservationDto.SearchCondition condition,
            String dateType,
            String dateSearchType) {
        AdminReservationDto.DateSearchType normalizedDateSearchType = parseDateSearchType(dateSearchType);

        // 프론트의 레거시 파라미터(dateType=reservation|show|RESERVATION|SHOW)도 수용
        if (normalizedDateSearchType == null) {
            normalizedDateSearchType = parseDateSearchType(dateType);
        }

        // showId 기반 조회에서 날짜 타입이 명시되지 않으면 공연일 기준으로 조회
        if (normalizedDateSearchType == null && condition.showId() != null) {
            normalizedDateSearchType = AdminReservationDto.DateSearchType.SHOW_DATE;
        }

        if (normalizedDateSearchType == null) {
            normalizedDateSearchType = condition.dateSearchType();
        }

        int page = Math.max(condition.page(), 0);
        int size = condition.size() > 0 ? condition.size() : 10;

        return new AdminReservationDto.SearchCondition(
                condition.searchType(),
                condition.searchKeyword(),
                condition.status(),
                condition.paymentStatus(),
                condition.showId(),
                condition.scheduleId(),
                condition.startDate(),
                condition.endDate(),
                normalizedDateSearchType,
                page,
                size);
    }

    private AdminReservationDto.DateSearchType parseDateSearchType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return null;
        }

        String normalized = rawType.trim().toUpperCase();
        return switch (normalized) {
            case "SHOW", "SHOW_DATE" -> AdminReservationDto.DateSearchType.SHOW_DATE;
            case "RESERVATION", "RESERVATION_DATE" -> AdminReservationDto.DateSearchType.RESERVATION_DATE;
            default -> null;
        };
    }

    @Override
    @GetMapping("/{reservationId}")
    public ResponseEntity<ApiResponse<PageResponse<AdminReservationDto.DetailResponse>>> getReservationDetails(
            @PathVariable Long reservationId) {
        PageResponse<AdminReservationDto.DetailResponse> response =
                adminReservationService.getReservationDetails(reservationId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
