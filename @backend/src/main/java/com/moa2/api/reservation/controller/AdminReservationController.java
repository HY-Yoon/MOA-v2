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
            @ParameterObject @ModelAttribute AdminReservationDto.SearchCondition condition) {

        // 최신순 정렬 (Service에서 쿼리 레벨로 정렬하지만, Pageable 객체 전달용)
        Pageable pageable = PageRequest.of(
                condition.page(),
                condition.size(),
                Sort.by(Sort.Direction.DESC, SORT_CREATED_AT));

        PageResponse<AdminReservationDto.ListResponse> response = adminReservationService.getReservations(
                condition,
                pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    @GetMapping("/detail")
    public ResponseEntity<ApiResponse<PageResponse<AdminReservationDto.DetailResponse>>> getReservationDetails(
            @RequestParam(required = false) Long reservationId,
            @RequestParam(required = false) Long showId,
            @RequestParam(required = false) Long scheduleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, SORT_CREATED_AT));
        PageResponse<AdminReservationDto.DetailResponse> response =
                adminReservationService.getReservationDetails(reservationId, showId, scheduleId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
