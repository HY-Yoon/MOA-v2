package com.moa2.api.show.controller.docs;

import com.moa2.api.show.dto.ShowDto;
import com.moa2.global.dto.ApiResponse;
import com.moa2.global.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "공연 조회 API", description = "사용자용 공연 조회 API")
public interface ShowControllerDocs {

        @Operation(summary = "공연 목록 조회", description = """
                        사용자가 예매 가능한 공연 목록을 조회합니다.

                        **자동 필터링:**
                        - 판매 허용(ALLOWED)된 공연만
                        - 판매중(ON_SALE) 또는 매진(SOLD_OUT) 상태만

                        **정렬 옵션:**
                        - `createdAt`: 최신 등록순 (기본)
                        - `startDate`: 공연 임박순
                        - `viewCount`: 인기순
                        - `title`: 가나다순
                        """)
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
        })
        ResponseEntity<ApiResponse<PageResponse<ShowDto.ListResponse>>> getShowList(
                        @ParameterObject @ModelAttribute ShowDto.ListRequest request);

        @Operation(summary = "공연 상세 조회", description = """
                        공연 상세 정보를 조회합니다.

                        **주의:** 조회 시 해당 공연의 조회수(viewCount)가 자동으로 1 증가합니다.
                        """)
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 - 존재하지 않는 공연", content = @Content(examples = @ExampleObject(value = "{\"success\":false, \"message\":\"공연을 찾을 수 없습니다\"}")))
        })
        ResponseEntity<ApiResponse<ShowDto.DetailResponse>> getShowDetail(
                        @Parameter(description = "공연 ID", required = true, example = "1") Long id);

        @Operation(summary = "날짜별 회차 조회", description = """
                        특정 공연(showId)의 회차 목록을 조회합니다.

                        - `date` 파라미터가 없으면 전체 회차 반환
                        - `date` 파라미터가 있으면 해당 날짜의 회차만 반환
                        - `isSoldOut`: 예약된 좌석 수 >= 전체 좌석 수 기준으로 계산됨
                        """)
        ResponseEntity<ApiResponse<List<ShowDto.ScheduleListResponse>>> getShowSchedules(
                        @Parameter(description = "공연 ID", required = true, example = "1") Long showId,

                        @Parameter(description = "공연 날짜 (YYYY-MM-DD, 선택)") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date);
}