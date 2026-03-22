package com.moa2.api.reservation.controller.docs;

import com.moa2.api.reservation.dto.AdminReservationDto;
import com.moa2.global.dto.ApiResponse;
import com.moa2.global.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "예매 관리 API", description = "관리자 예매 관리 API")
public interface AdminReservationControllerDocs {

        @Operation(
                summary = "예매 목록 조회",
                description = """
                        관리자용 전체 예매 내역을 조회합니다.

                        **모든 파라미터는 선택(optional)이며, 제공한 것만 AND 조건으로 필터링됩니다.**

                        **검색/필터 조건:**
                        - `status` — 예매 상태 (PENDING / CONFIRMED / CANCELLED / SOLD)
                        - `paymentStatus` — 결제 상태 (COMPLETED / FAILED / CANCELLED 등)
                        - `searchType` + `searchKeyword` — 예매번호 / 예매자명 / 예매자ID / 공연 제목 검색
                        - `startDate` / `endDate` + `dateSearchType` — 날짜 범위 필터 (예매일 또는 공연일 기준)

                        **정렬:** 최신 예매순 (createdAt DESC)
                        """)
        @Parameters({
                @Parameter(name = "searchType",      in = ParameterIn.QUERY, required = false,
                        description = "검색 유형",
                        schema = @Schema(allowableValues = {"RESERVATION_NUMBER", "BOOKER_NAME", "BOOKER_ID", "SHOW_TITLE"})),
                @Parameter(name = "searchKeyword",   in = ParameterIn.QUERY, required = false,
                        description = "검색어 (예매번호, 예매자명, 예매자ID, 공연제목)"),
                @Parameter(name = "showId",          in = ParameterIn.QUERY, required = false,
                        description = "공연 ID (해당 공연 기준 목록 조회)", example = "1",
                        schema = @Schema(type = "integer", format = "int64")),
                @Parameter(name = "status",          in = ParameterIn.QUERY, required = false,
                        description = "예매 상태",
                        schema = @Schema(allowableValues = {"PENDING", "CONFIRMED", "CANCELLED", "SOLD"})),
                @Parameter(name = "paymentStatus",   in = ParameterIn.QUERY, required = false,
                        description = "결제 상태",
                        schema = @Schema(allowableValues = {"COMPLETED", "FAILED", "CANCELLED", "PENDING"})),
                @Parameter(name = "startDate",       in = ParameterIn.QUERY, required = false,
                        description = "조회 시작일시 (ISO 8601, yyyy-MM-dd'T'HH:mm:ss)", example = "2026-01-01T00:00:00",
                        schema = @Schema(type = "string", format = "date-time")),
                @Parameter(name = "endDate",         in = ParameterIn.QUERY, required = false,
                        description = "조회 종료일시 (ISO 8601, yyyy-MM-dd'T'HH:mm:ss)", example = "2026-12-31T23:59:59",
                        schema = @Schema(type = "string", format = "date-time")),
                @Parameter(name = "dateSearchType",  in = ParameterIn.QUERY, required = false,
                        description = "날짜 기준 (기본값: RESERVATION_DATE)",
                        schema = @Schema(allowableValues = {"RESERVATION_DATE", "SHOW_DATE"})),
                @Parameter(name = "page",            in = ParameterIn.QUERY, required = false,
                        description = "페이지 번호 (0부터 시작, 기본값: 0)", example = "0",
                        schema = @Schema(type = "integer", defaultValue = "0")),
                @Parameter(name = "size",            in = ParameterIn.QUERY, required = false,
                        description = "페이지 크기 (기본값: 10)", example = "10",
                        schema = @Schema(type = "integer", defaultValue = "10")),
        })
        ResponseEntity<ApiResponse<PageResponse<AdminReservationDto.ListResponse>>> getReservations(
                        @ParameterObject @ModelAttribute AdminReservationDto.SearchCondition condition);

        @Operation(
                summary = "예매 상세 조회",
                description = """
                        예매 ID(reservationId) 기준으로 예매 상세를 조회합니다.

                        `reservationId`는 필수값입니다.

                        **응답 정보 (예매별):**
                        - 예매 기본 정보 (예매번호, 예매일시, 상태)
                        - 공연 / 일정 / 장소 정보
                        - 좌석 목록 (구역·행·번호·가격)
                        - 예매자 정보
                        - 결제 정보
                        """)
        @Parameters({
                @Parameter(name = "reservationId", in = ParameterIn.PATH, required = true,
                        description = "예매 ID (해당 예매 단건 상세 조회)", example = "1",
                        schema = @Schema(type = "integer", format = "int64"))
        })
        ResponseEntity<ApiResponse<PageResponse<AdminReservationDto.DetailResponse>>> getReservationDetails(
                        @PathVariable Long reservationId);
}
