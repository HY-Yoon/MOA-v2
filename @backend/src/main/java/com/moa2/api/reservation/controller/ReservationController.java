package com.moa2.api.reservation.controller;

import com.moa2.api.reservation.controller.docs.ReservationControllerDocs;
import com.moa2.api.reservation.dto.ReservationDto;
import com.moa2.api.reservation.dto.ReservationSearchCondition;
import com.moa2.api.reservation.service.ReservationService;
import com.moa2.global.dto.ApiResponse;
import com.moa2.global.dto.PageResponse;
import com.moa2.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 마이페이지 - 예매 내역 API
 * Cookie 기반 인증 사용
 */
@Slf4j
@RestController
@Profile("v1")
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController implements ReservationControllerDocs {

  private final ReservationService reservationService;

  @Override
  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<ReservationDto.ListResponse>>> getMyReservations(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @ModelAttribute ReservationSearchCondition condition) {

    // 최신순 정렬
    Pageable pageable = PageRequest.of(condition.page(), condition.size(), Sort.by(Sort.Direction.DESC, "createdAt"));

    PageResponse<ReservationDto.ListResponse> response = reservationService.getMyReservations(userPrincipal.getEmail(),
        condition, pageable);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @Override
  @GetMapping("/{reservationId}")
  public ResponseEntity<ApiResponse<ReservationDto.DetailResponse>> getReservationDetail(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @PathVariable Long reservationId) {

    ReservationDto.DetailResponse response = reservationService.getReservationDetail(userPrincipal.getEmail(),
        reservationId);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @Override
  @DeleteMapping("/{reservationId}")
  public ResponseEntity<ApiResponse<ReservationDto.CancelResponse>> cancelReservation(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @PathVariable Long reservationId) {

    ReservationDto.CancelResponse response = reservationService.cancelReservation(userPrincipal.getEmail(),
        reservationId);
    return ResponseEntity.ok(ApiResponse.success(response, response.message()));
  }
}
