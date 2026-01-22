package com.moa2.api.reservation.controller;

import com.moa2.api.reservation.dto.ReservationCancelResponse;
import com.moa2.api.reservation.dto.ReservationDetailResponse;
import com.moa2.api.reservation.dto.ReservationListResponse;
import com.moa2.api.reservation.service.ReservationService;
import com.moa2.global.dto.ApiResponse;
import com.moa2.global.dto.PageResponse;
import com.moa2.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 마이페이지 - 예매 내역 API
 * Cookie 기반 인증 사용
 */
@Slf4j
@Tag(name = "마이페이지 - 예매 내역", description = "내 예매 내역 조회 및 취소 API")
@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * 내 예매 내역 목록 조회
     */
    @Operation(
        summary = "내 예매 내역 목록 조회",
        description = "로그인한 사용자의 예매 내역을 조회합니다.\n\n" +
                     "**인증 방식:** Cookie (accessToken)\n\n" +
                     "**필터링:**\n" +
                     "- `status` 파라미터로 예매 상태별 조회 가능\n" +
                     "  - CONFIRMED: 확정된 예매\n" +
                     "  - CANCELLED: 취소된 예매\n" +
                     "  - 미지정: 모든 예매\n\n" +
                     "**정렬:** 최신 예매순 (createdAt DESC)\n\n" +
                     "**페이지네이션:** page, size 파라미터 사용"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "성공 응답",
                    value = """
                    {
                      "success": true,
                      "data": {
                        "content": [
                          {
                            "reservationId": 1,
                            "reservationNumber": "R20260115-001",
                            "reservationDate": "2026-01-15T10:30:00",
                            "reservationStatus": "CONFIRMED",
                            "paymentStatus": "COMPLETED",
                            "show": {
                              "showId": 1,
                              "title": "레미제라블",
                              "posterUrl": "/images/posters/lesmiserables.jpg",
                              "genre": "MUSICAL"
                            },
                            "schedule": {
                              "scheduleId": 1,
                              "showDate": "2026-02-20",
                              "showTime": "19:00",
                              "location": {
                                "region": "SEOUL",
                                "venue": "예술의전당",
                                "hallName": "오페라극장"
                              }
                            },
                            "seatCount": 2,
                            "totalAmount": 300000,
                            "canCancel": true
                          }
                        ],
                        "currentPage": 0,
                        "totalPages": 1,
                        "totalCount": 1,
                        "pageSize": 10
                      },
                      "message": null
                    }
                    """
                )
            )
        )
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReservationListResponse>>> getMyReservations(
            @Parameter(description = "예매 상태 필터 (CONFIRMED, CANCELLED)")
            @RequestParam(required = false) String status,
            @Parameter(description = "페이지 번호 (0부터 시작)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "10") int size) {
        
        // SecurityContext에서 인증된 사용자 이메일 가져오기
        String email = getAuthenticatedUserEmail();
        
        // 최신순 정렬
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        PageResponse<ReservationListResponse> response = reservationService.getMyReservations(email, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 예매 상세 조회
     */
    @Operation(
        summary = "예매 상세 조회",
        description = "예매 ID로 상세 정보를 조회합니다.\n\n" +
                     "**인증 방식:** Cookie (accessToken)\n\n" +
                     "**응답 정보:**\n" +
                     "- 예매 기본 정보 (예매번호, 예매일시, 상태)\n" +
                     "- 공연 정보 (제목, 포스터, 장르, 출연진 등)\n" +
                     "- 일정 및 장소 정보\n" +
                     "- 좌석 정보 (구역, 행, 번호, 가격)\n" +
                     "- 예매자 정보\n" +
                     "- 결제 정보 (결제 방법, 금액, 상태)\n" +
                     "- 취소 가능 여부\n\n" +
                     "**권한:** 본인의 예매만 조회 가능"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "성공 응답",
                    value = """
                    {
                      "success": true,
                      "data": {
                        "reservationId": 1,
                        "reservationNumber": "R20260115-001",
                        "reservationDate": "2026-01-15T10:30:00",
                        "reservationStatus": "CONFIRMED",
                        "show": {
                          "showId": 1,
                          "title": "레미제라블",
                          "posterUrl": "/images/posters/lesmiserables.jpg",
                          "genre": "MUSICAL",
                          "runningTime": "150분",
                          "cast": "김철수, 이영희, 박민수"
                        },
                        "schedule": {
                          "scheduleId": 1,
                          "showDate": "2026-02-20",
                          "showTime": "19:00",
                          "location": {
                            "region": "SEOUL",
                            "venue": "예술의전당",
                            "hallName": "오페라극장",
                            "address": "서울특별시 서초구 남부순환로 2406"
                          }
                        },
                        "seats": [
                          {
                            "seatId": 1,
                            "section": "VIP석",
                            "row": "A",
                            "number": 1,
                            "price": 150000
                          },
                          {
                            "seatId": 2,
                            "section": "VIP석",
                            "row": "A",
                            "number": 2,
                            "price": 150000
                          }
                        ],
                        "seatCount": 2,
                        "booker": {
                          "name": "오늘다",
                          "phone": "010-1234-5678",
                          "email": "todayda1006@gmail.com"
                        },
                        "payment": {
                          "orderId": "ORD20260115-001",
                          "paymentKey": "tviva20260115abc123",
                          "totalAmount": 300000,
                          "paymentMethod": "CARD",
                          "paymentStatus": "COMPLETED",
                          "paidAt": "2026-01-15T10:31:00"
                        },
                        "canCancel": true,
                        "cancelledAt": null
                      },
                      "message": null
                    }
                    """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "예매를 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "예매 없음",
                    value = """
                    {
                      "success": false,
                      "data": null,
                      "message": "예매 정보를 찾을 수 없습니다: 999"
                    }
                    """
                )
            )
        )
    })
    @GetMapping("/{reservationId}")
    public ResponseEntity<ApiResponse<ReservationDetailResponse>> getReservationDetail(
            @Parameter(description = "예매 ID", required = true)
            @PathVariable Long reservationId) {
        
        try {
            // SecurityContext에서 인증된 사용자 이메일 가져오기
            String email = getAuthenticatedUserEmail();
            
            ReservationDetailResponse response = reservationService.getReservationDetail(email, reservationId);
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (IllegalArgumentException e) {
            log.warn("예매 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 예매 취소
     */
    @Operation(
        summary = "예매 취소",
        description = "예매를 취소합니다.\n\n" +
                     "**인증 방식:** Cookie (accessToken)\n\n" +
                     "**취소 조건:**\n" +
                     "- CONFIRMED 상태의 예매만 취소 가능\n" +
                     "- 공연일 2일 전까지만 취소 가능\n" +
                     "- 본인의 예매만 취소 가능\n\n" +
                     "**처리 과정:**\n" +
                     "1. 예매 상태를 CANCELLED로 변경\n" +
                     "2. 결제 상태를 CANCELLED로 변경\n" +
                     "3. 취소일시 기록\n\n" +
                     "**주의:** 취소 후에는 복구할 수 없습니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "취소 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "성공 응답",
                    value = """
                    {
                      "success": true,
                      "data": {
                        "reservationId": 1,
                        "reservationNumber": "R20260115-001",
                        "message": "예매가 취소되었습니다."
                      },
                      "message": "예매가 취소되었습니다."
                    }
                    """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "취소 실패",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "이미 취소된 예매",
                        value = """
                        {
                          "success": false,
                          "data": null,
                          "message": "이미 취소된 예매입니다."
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "취소 기한 만료",
                        value = """
                        {
                          "success": false,
                          "data": null,
                          "message": "취소 가능 기한이 지났습니다. (공연일 2일 전까지 취소 가능)"
                        }
                        """
                    )
                }
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "예매를 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "예매 없음",
                    value = """
                    {
                      "success": false,
                      "data": null,
                      "message": "예매 정보를 찾을 수 없습니다: 999"
                    }
                    """
                )
            )
        )
    })
    @DeleteMapping("/{reservationId}")
    public ResponseEntity<ApiResponse<ReservationCancelResponse>> cancelReservation(
            @Parameter(description = "예매 ID", required = true)
            @PathVariable Long reservationId) {
        
        try {
            // SecurityContext에서 인증된 사용자 이메일 가져오기
            String email = getAuthenticatedUserEmail();
            
            ReservationCancelResponse response = reservationService.cancelReservation(email, reservationId);
            return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
            
        } catch (IllegalStateException e) {
            // 취소 불가능한 상태
            log.warn("예매 취소 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
                    
        } catch (IllegalArgumentException e) {
            // 예매를 찾을 수 없음
            log.error("예매 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * SecurityContext에서 인증된 사용자의 이메일을 가져옴
     */
    private String getAuthenticatedUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() 
                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            throw new IllegalStateException("인증이 필요합니다.");
        }
        
        Object principalObj = authentication.getPrincipal();
        if (principalObj instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getEmail();
        }
        return (String) principalObj;
    }
}
