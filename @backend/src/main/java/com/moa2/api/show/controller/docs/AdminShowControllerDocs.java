package com.moa2.api.show.controller.docs;

import com.moa2.api.show.dto.ShowDto;
import com.moa2.global.dto.ApiResponse;
import com.moa2.global.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "공연 관리 API", description = "관리자용 공연 관리 API")
public interface AdminShowControllerDocs {

    String DESC_CREATE_SHOW = """
            새로운 공연을 등록합니다.

            **요청 형식:** multipart/form-data

            **필수 필드:**
            - `data`: 공연 정보 (JSON)
            - `poster`: 포스터 이미지 파일

            **선택 필드:**
            - `detailImages`: 상세 이미지 파일 목록 (여러 개 가능)

            **장르 (genre):** MUSICAL, CONCERT, THEATER, CLASSIC, DANCE
            """;

    String DESC_UPDATE_SHOW = """
            공연 정보를 수정합니다.

            **요청 형식:** multipart/form-data

            **수정 규칙:**
            - **WAITING 상태:** 모든 필드 수정 가능
            - **ON_SALE 이후:** 제한된 필드만 수정 가능
              - 수정 가능: 제목, 상영시간, 출연진, 포스터, 상세이미지, **일정 수정**
              - 수정 불가: 장르, 장소, 예매 시작일

            **일정 관련:**
            - 일정 추가/수정/삭제: 모두 이 API에서 처리
            - 팝업에서 변경한 모든 일정을 `schedules`에 포함하여 '완료' 버튼 클릭 시 최종 저장
            - 추가: `scheduleId` 없음 (null)
            - 수정: `scheduleId` 있음
            - 삭제: `deletedScheduleIds`에 포함
            - ON_SALE 이후: 예매된 좌석이 없는 경우에만 수정/삭제 가능

            **상세 이미지 관련:**
            - 이미지 추가: `detailImages`에 새 파일 포함
            - 이미지 삭제: `deletedDetailImageIds`에 삭제할 이미지 ID 포함
            - 수정 폼에서 변경한 모든 이미지를 '완료' 버튼 클릭 시 최종 저장

            **주의:** 수정할 필드만 포함하면 됩니다.
            """;

    String DESC_UPDATE_SHOW_JSON = """
            공연 정보 (JSON 문자열, 수정할 필드만 포함)

            **수정 가능 필드:**
            - `title`: 공연 제목
            - `runningTime`: 상영 시간 (문자열, 예: "150분")
            - `cast`: 출연진 정보
            - `genre`: 장르 (WAITING 상태에서만 수정 가능)
            - `location`: 장소 정보 (WAITING 상태에서만 수정 가능)
              - `region`: 지역 (예: "서울")
              - `venueName`: 공연장명 (예: "올림픽공원")
              - `hallName`: 홀명 (예: "KSPO DOME")
            - `saleStartDate`: 예매 시작일시 (WAITING 상태에서만 수정 가능)
            - `schedules`: 일정 목록 (추가/수정 모두 포함)
            - `deletedScheduleIds`: 삭제할 일정 ID 목록
            - `deletedDetailImageIds`: 삭제할 상세 이미지 ID 목록

            **일정 처리:**
            - 추가: `scheduleId` 없음
            - 수정: `scheduleId` 있음
            - 삭제: `deletedScheduleIds`에 포함
            - ON_SALE 이후: 예매된 좌석이 없는 경우에만 수정/삭제 가능

            **상세 이미지 처리:**
            - 추가: `detailImages` 파일 업로드
            - 삭제: `deletedDetailImageIds`에 포함
            """;

    @Operation(summary = "공연 목록 조회")
    ResponseEntity<ApiResponse<PageResponse<ShowDto.AdminListResponse>>> getShowList(
            @ModelAttribute ShowDto.AdminListRequest request);

    @Operation(summary = "공연 상세 조회", description = "관리자용 공연 상세 정보를 조회합니다.")
    ResponseEntity<ApiResponse<ShowDto.AdminDetailResponse>> getShowDetail(
            @Parameter(description = "공연 ID", required = true) @PathVariable Long id);

    @Operation(summary = "공연 등록", description = DESC_CREATE_SHOW)
    ResponseEntity<ApiResponse<ShowDto.CreateResponse>> createShow(
            @Parameter(description = "공연 정보 (JSON)", required = true, schema = @Schema(implementation = ShowDto.CreateRequest.class)) @RequestPart("data") String dataJson,
            @Parameter(description = "포스터 이미지 파일 (jpg, jpeg, png, gif, webp, 최대 1GB)", required = true) @RequestPart("poster") MultipartFile poster,
            @Parameter(description = "상세 이미지 파일 목록 (선택, 여러 개 가능)") @RequestPart(value = "detailImages", required = false) List<MultipartFile> detailImages);

    @Operation(summary = "공연 수정", description = DESC_UPDATE_SHOW_JSON)
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "성공 응답", value = "{\"success\":true,\"data\":{\"showId\":1,\"message\":\"공연이 수정되었습니다\"},\"message\":\"공연이 수정되었습니다\"}"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "수정 실패 - ON_SALE 상태에서 제한된 필드 수정 시도", content = @Content(mediaType = "application/json", examples = {
                    @ExampleObject(name = "장르 수정 시도", value = "{\"success\":false,\"data\":null,\"message\":\"판매중인 공연은 장르를 수정할 수 없습니다\"}"),
                    @ExampleObject(name = "장소 수정 시도", value = "{\"success\":false,\"data\":null,\"message\":\"판매중인 공연은 장소를 수정할 수 없습니다\"}"),
                    @ExampleObject(name = "예매 시작일 수정 시도", value = "{\"success\":false,\"data\":null,\"message\":\"판매중인 공연은 예매 시작일을 수정할 수 없습니다\"}"),
                    @ExampleObject(name = "예매된 좌석이 있는 일정 수정 시도", value = "{\"success\":false,\"data\":null,\"message\":\"예매된 좌석이 있는 스케줄은 수정할 수 없습니다. 스케줄 ID: 1\"}")
            }))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "multipart/form-data 요청", required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
    ResponseEntity<ApiResponse<ShowDto.UpdateResponse>> updateShow(
            @Parameter(description = "공연 ID", required = true) @PathVariable Long id,
            @Parameter(required = true, schema = @Schema(implementation = ShowDto.UpdateRequest.class), examples = {
                    @ExampleObject(name = "제목 및 출연진 수정", value = "{\"title\":\"레미제라블 (수정)\",\"cast\":\"김철수, 이영희, 박민수\"}"),
                    @ExampleObject(name = "상영시간 수정", value = "{\"runningTime\":\"160분\"}"),
                    @ExampleObject(name = "WAITING 상태 - 전체 수정", value = "{\"title\":\"레미제라블\",\"genre\":\"MUSICAL\",\"location\":{\"region\":\"서울\",\"venueName\":\"올림픽공원\",\"hallName\":\"KSPO DOME\"},\"saleStartDate\":\"2026-01-01T00:00:00\"}"),
                    @ExampleObject(name = "일정 추가/수정/삭제", value = "{\"schedules\":[{\"scheduleId\":1,\"showDate\":\"2026-01-20\",\"showTime\":\"19:00\",\"ticketOpenTime\":\"2026-01-01T10:00:00\"},{\"showDate\":\"2026-01-25\",\"showTime\":\"19:00\",\"ticketOpenTime\":\"2026-01-01T10:00:00\"}],\"deletedScheduleIds\":[2,3]}"),
                    @ExampleObject(name = "상세 이미지 삭제", value = "{\"deletedDetailImageIds\":[1,3,5]}")
            }) @RequestPart("data") String dataJson,
            @Parameter(description = "포스터 이미지 파일 (선택, 포스터 교체 시에만 포함)") @RequestPart(value = "poster", required = false) MultipartFile poster,
            @Parameter(description = "상세 이미지 파일 목록 (선택, 새 이미지 추가 시에만 포함. 삭제는 deletedDetailImageIds 사용)") @RequestPart(value = "detailImages", required = false) List<MultipartFile> detailImages)
            throws Exception;

    @Operation(summary = "공연 삭제", description = "공연 삭제 : WAITING 상태의 공연만 삭제 가능합니다.")
    ResponseEntity<ApiResponse<ShowDto.DeleteResponse>> deleteShow(
            @Parameter(description = "공연 ID", required = true) @PathVariable Long id);

    @Operation(summary = "공연 판매 상태 변경", description = "공연의 판매 상태를 변경합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상태 변경 성공", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "성공 응답", value = "{\"success\":true,\"data\":{\"showId\":2,\"saleStatus\":\"SUSPENDED\",\"message\":\"판매 상태가 변경되었습니다\"},\"message\":\"판매 상태가 변경되었습니다\"}"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "에러 응답", value = "{\"success\":false,\"data\":null,\"message\":\"유효하지 않은 판매 상태입니다\"}")))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "판매 상태 변경 요청", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ShowDto.SaleStatusUpdateRequest.class), examples = {
            @ExampleObject(name = "SUSPENDED 예시", value = "{\"saleStatus\": \"SUSPENDED\"}"),
            @ExampleObject(name = "ALLOWED 예시", value = "{\"saleStatus\": \"ALLOWED\"}")
    }))
    ResponseEntity<ApiResponse<ShowDto.SaleStatusUpdateResponse>> updateSaleStatus(
            @Parameter(description = "공연 ID", required = true) @PathVariable Long id,
            @RequestBody ShowDto.SaleStatusUpdateRequest showSaleResponse);
}
