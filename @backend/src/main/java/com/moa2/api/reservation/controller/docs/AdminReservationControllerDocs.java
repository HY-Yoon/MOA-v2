package com.moa2.api.reservation.controller.docs;

import com.moa2.api.reservation.dto.AdminReservationDto;
import com.moa2.global.dto.ApiResponse;
import com.moa2.global.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "예매 관리 API", description = "관리자 예매 관리 API")
public interface AdminReservationControllerDocs {

        @Operation(summary = "예매 목록 조회", description = "모든 예매 내역을 조회합니다. (페이징, 상태 필터링, 검색, 날짜 범위 지원)")
        ResponseEntity<ApiResponse<PageResponse<AdminReservationDto.ListResponse>>> getReservations(
                        @ParameterObject @ModelAttribute AdminReservationDto.SearchCondition condition);

        @Operation(summary = "예매 상세 조회", description = "특정 예매 건의 상세 정보를 조회합니다.")
        ResponseEntity<ApiResponse<AdminReservationDto.DetailResponse>> getReservationDetail(
                        @Parameter(description = "예매 ID", example = "1") @PathVariable Long reservationId);
}
