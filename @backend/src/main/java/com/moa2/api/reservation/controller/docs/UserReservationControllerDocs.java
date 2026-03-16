package com.moa2.api.reservation.controller.docs;

import com.moa2.api.reservation.dto.ReservationDto;
import com.moa2.api.reservation.dto.ReservationSearchCondition;
import com.moa2.global.dto.ApiResponse;
import com.moa2.global.dto.PageResponse;
import com.moa2.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "사용자 예매 API", description = "내 예매 내역 조회 및 상세 조회 API")
public interface UserReservationControllerDocs {

  @Operation(summary = "내 예매 내역 목록 조회", description = "로그인한 사용자의 예매 내역을 조회합니다.\n\n" +
      "**인증 방식:** Cookie (accessToken)\n\n" +
      "**필터링:**\n" +
      "- `status` 파라미터로 예매 상태별 조회 가능\n" +
      "  - CONFIRMED: 확정된 예매\n" +
      "  - CANCELLED: 취소된 예매\n" +
      "  - 미지정: 모든 예매\n" +
      "- `paymentStatus` 파라미터로 결제 상태별 조회 가능\n" +
      "- `startDate`, `endDate` 파라미터로 기간별 조회 가능\n" +
      "- `dateType` 파라미터로 날짜 기준 선택 (RESERVATION: 예매일, SHOW: 공연일)\n\n" +
      "**정렬:** 최신 예매순 (createdAt DESC)\n\n" +
      "**페이지네이션:** page, size 파라미터 사용 (기본값: page=0, size=20)")
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "성공 응답", value = """
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
              "pageSize": 20
            },
            "message": null
          }
          """)))
  })
  ResponseEntity<ApiResponse<PageResponse<ReservationDto.ListResponse>>> getMyReservations(
      @Parameter(hidden = true) UserPrincipal userPrincipal,
      @ParameterObject ReservationSearchCondition condition);

  @Operation(summary = "예매 상세 조회", description = "예매 ID로 상세 정보를 조회합니다.\n\n" +
      "**인증 방식:** Cookie (accessToken)\n\n" +
      "**응답 정보:**\n" +
      "- 예매 기본 정보 (예매번호, 예매일시, 상태)\n" +
      "- 공연 정보 (제목, 포스터, 장르, 출연진 등)\n" +
      "- 일정 및 장소 정보\n" +
      "- 좌석 정보 (구역, 행, 번호, 가격)\n" +
      "- 예매자 정보\n" +
      "- 결제 정보 (결제 방법, 금액, 상태)\n" +
      "- 취소 가능 여부\n\n" +
      "**권한:** 본인의 예매만 조회 가능")
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "성공 응답", value = """
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
                  "sectionName": "VIP석",
                  "row": "A",
                  "number": 1,
                  "price": 150000
                },
                {
                  "sectionName": "VIP석",
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
          """))),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "예매를 찾을 수 없음", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "예매 없음", value = """
          {
            "success": false,
            "data": null,
            "message": "예매 정보를 찾을 수 없습니다."
          }
          """)))
  })
  ResponseEntity<ApiResponse<ReservationDto.DetailResponse>> getReservationDetail(
      @Parameter(hidden = true) UserPrincipal userPrincipal,
      @Parameter(description = "예매 ID", required = true) @PathVariable Long reservationId);

  @Operation(summary = "예매 취소", description = "예매를 취소합니다.\n\n" +
      "**인증 방식:** Cookie (accessToken)\n\n" +
      "**취소 조건:**\n" +
      "- CONFIRMED, SOLD 상태의 예매만 취소 가능\n" +
      "- 공연일 2일 전까지만 취소 가능\n" +
      "- 본인의 예매만 취소 가능\n\n" +
      "**처리 과정:**\n" +
      "1. 예매 상태를 CANCELLED로 변경\n" +
      "2. 결제 상태를 CANCELLED로 변경, 예매 확정 후 결제 못한 결우는 FAILED\n" +
      "3. 취소일시 기록\n\n" +
      "**주의:** 취소 후에는 복구할 수 없습니다.")
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "취소 성공", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "성공 응답", value = """
          {
            "success": true,
            "data": {
              "reservationId": 1,
              "reservationNumber": "R20260115-001",
              "message": "예매가 취소되었습니다."
            },
            "message": "예매가 취소되었습니다."
          }
          """))),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "취소 실패", content = @Content(mediaType = "application/json", examples = {
          @ExampleObject(name = "이미 취소된 예매", value = """
              {
                "success": false,
                "data": null,
                "message": "이미 취소된 예매입니다."
              }
              """),
          @ExampleObject(name = "취소 기한 만료", value = """
              {
                "success": false,
                "data": null,
                "message": "취소 가능 기한이 지났습니다. (공연일 2일 전까지 취소 가능)"
              }
              """)
      })),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "예매를 찾을 수 없음", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "예매 없음", value = """
          {
            "success": false,
            "data": null,
            "message": "예매 정보를 찾을 수 없습니다"
          }
          """)))
  })
  ResponseEntity<ApiResponse<ReservationDto.CancelResponse>> cancelReservation(
      @Parameter(hidden = true) UserPrincipal userPrincipal,
      @Parameter(description = "예매 ID", required = true) @PathVariable Long reservationId);
}
