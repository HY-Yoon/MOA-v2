package com.moa2.api.admin.show.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.moa2.api.admin.show.dto.*;
import com.moa2.api.admin.show.service.AdminShowService;
import com.moa2.global.dto.ApiResponse;
import com.moa2.global.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.bind.annotation.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "공연 관리 API", description = "관리자용 공연 관리 API")
@RestController
@RequestMapping("/api/v1/admin/shows")
@RequiredArgsConstructor
public class AdminShowController {

    private final AdminShowService adminShowService;
    private final Validator validator;

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(com.fasterxml.jackson.databind.DeserializationFeature.READ_ENUMS_USING_TO_STRING, false)
        .configure(com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true);


    @Operation(
        summary = "공연 목록 조회"
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ShowListResponse>>> getShowList(
            @Parameter(description = "공연 상태 (WAITING, ON_SALE, SOLD_OUT, ENDED, SUSPENDED)")
            @RequestParam(required = false) String showStatus,
            @Parameter(description = "판매 상태 (ALLOWED, SUSPENDED)")
            @RequestParam(required = false) String saleStatus,
            @Parameter(description = "검색 키워드 (공연 제목)")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "공연 시작일 (YYYY-MM-DD 형식)")
            @RequestParam(required = false) java.time.LocalDate startDate,
            @Parameter(description = "공연 종료일 (YYYY-MM-DD 형식)")
            @RequestParam(required = false) java.time.LocalDate endDate,
            @Parameter(description = "정렬 기준 (필드명,방향 createdAt,desc)")
            @RequestParam String sort,
            @Parameter(description = "페이지 번호 (0부터 시작)")
            @RequestParam int page,
            @Parameter(description = "페이지 크기")
            @RequestParam int size) {
        
        ShowListRequest request = new ShowListRequest();
        if (showStatus != null) {
            request.setShowStatus(com.moa2.global.model.ShowStatus.valueOf(showStatus));
        }
        if (saleStatus != null) {
            request.setSaleStatus(com.moa2.global.model.SaleStatus.valueOf(saleStatus));
        }
        request.setKeyword(keyword);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setSort(sort);
        request.setPage(page);
        request.setSize(size);

        // Sort 파싱
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && "desc".equalsIgnoreCase(sortParts[1])
            ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        Page<ShowListResponse> pageResult = adminShowService.getShowList(request, pageable);
        PageResponse<ShowListResponse> result = PageResponse.of(pageResult);
        return ResponseEntity.ok(ApiResponse.success(result));
    }


    @Operation(summary = "공연 상세 조회", description = "관리자용 공연 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShowDetailResponse>> getShowDetail(
            @Parameter(description = "공연 ID", required = true) @PathVariable Long id) {
        ShowDetailResponse result = adminShowService.getShowDetail(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(
        summary = "공연 등록", 
        description = "새로운 공연을 등록합니다.\n\n" +
                     "**요청 형식:** multipart/form-data\n\n" +
                     "**필수 필드:**\n" +
                     "- `data`: 공연 정보 (JSON)\n" +
                     "- `poster`: 포스터 이미지 파일\n\n" +
                     "**선택 필드:**\n" +
                     "- `detailImages`: 상세 이미지 파일 목록 (여러 개 가능)\n\n" +
                     "**장르 (genre):** MUSICAL, CONCERT, THEATER, CLASSIC, DANCE"
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ShowCreateResponse>> createShow(
            @Parameter(
                description = "공연 정보 (JSON)",
                required = true,
                schema = @Schema(implementation = ShowCreateRequest.class)
            )
            @RequestPart(value = "data", required = true) @Valid ShowCreateRequest request,
            @Parameter(
                description = "포스터 이미지 파일 (jpg, jpeg, png, gif, webp, 최대 10MB)", 
                required = true
            )
            @RequestPart("poster") MultipartFile poster,
            @Parameter(description = "상세 이미지 파일 목록 (선택, 여러 개 가능)")
            @RequestPart(value = "detailImages", required = false) List<MultipartFile> detailImages) {
        
        ShowCreateResponse result = adminShowService.createShow(request, poster, detailImages);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(result, result.getMessage()));
    }

    @Operation(
        summary = "공연 수정", 
        description = "공연 정보를 수정합니다.\n\n" +
                     "**요청 형식:** multipart/form-data\n\n" +
                     "**수정 규칙:**\n" +
                     "- **WAITING 상태:** 모든 필드 수정 가능\n" +
                     "- **ON_SALE 이후:** 제한된 필드만 수정 가능\n" +
                     "  - 수정 가능: 제목, 상영시간, 출연진, 포스터, 상세이미지, **일정 수정**\n" +
                     "  - 수정 불가: 장르, 장소, 예매 시작일\n\n" +
                     "**일정 관련:**\n" +
                     "- 일정 추가/수정/삭제: 모두 이 API에서 처리\n" +
                     "- 팝업에서 변경한 모든 일정을 `schedules`에 포함하여 '완료' 버튼 클릭 시 최종 저장\n" +
                     "- 추가: `scheduleId` 없음 (null)\n" +
                     "- 수정: `scheduleId` 있음\n" +
                     "- 삭제: `deletedScheduleIds`에 포함\n" +
                     "- ON_SALE 이후: 예매된 좌석이 없는 경우에만 수정/삭제 가능\n\n" +
                     "**주의:** 수정할 필드만 포함하면 됩니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "수정 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "성공 응답",
                    value = "{\"success\":true,\"data\":{\"showId\":1,\"message\":\"공연이 수정되었습니다\"},\"message\":\"공연이 수정되었습니다\"}"
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "수정 실패 - ON_SALE 상태에서 제한된 필드 수정 시도",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "장르 수정 시도",
                        value = "{\"success\":false,\"data\":null,\"message\":\"판매중인 공연은 장르를 수정할 수 없습니다\"}"
                    ),
                    @ExampleObject(
                        name = "장소 수정 시도",
                        value = "{\"success\":false,\"data\":null,\"message\":\"판매중인 공연은 장소를 수정할 수 없습니다\"}"
                    ),
                    @ExampleObject(
                        name = "예매 시작일 수정 시도",
                        value = "{\"success\":false,\"data\":null,\"message\":\"판매중인 공연은 예매 시작일을 수정할 수 없습니다\"}"
                    ),
                    @ExampleObject(
                        name = "예매된 좌석이 있는 일정 수정 시도",
                        value = "{\"success\":false,\"data\":null,\"message\":\"예매된 좌석이 있는 스케줄은 수정할 수 없습니다. 스케줄 ID: 1\"}"
                    )
                }
            )
        )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "multipart/form-data 요청",
        required = true,
        content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
    )
    @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ShowUpdateResponse>> updateShow(
            @Parameter(description = "공연 ID", required = true) @PathVariable Long id,
            @Parameter(
                description = "공연 정보 (JSON 문자열, 수정할 필드만 포함)\n\n" +
                            "**수정 가능 필드:**\n" +
                            "- `title`: 공연 제목\n" +
                            "- `runningTime`: 상영 시간(분)\n" +
                            "- `cast`: 출연진 정보\n" +
                            "- `genre`: 장르 (WAITING 상태에서만 수정 가능)\n" +
                            "- `location`: 장소 정보 (WAITING 상태에서만 수정 가능)\n" +
                            "  - `region`: 지역 (예: \"경기\")\n" +
                            "  - `venue`: 공연장명 (예: \"예술의전당\")\n" +
                            "  - `hall`: 홀명 (예: \"A홀\")\n" +
                            "- `saleStartDate`: 예매 시작일시 (WAITING 상태에서만 수정 가능)\n" +
                            "- `schedules`: 일정 목록 (추가/수정 모두 포함)\n" +
                            "- `deletedScheduleIds`: 삭제할 일정 ID 목록\n\n" +
                            "**일정 처리:**\n" +
                            "- 추가: `scheduleId` 없음\n" +
                            "- 수정: `scheduleId` 있음\n" +
                            "- 삭제: `deletedScheduleIds`에 포함\n" +
                            "- ON_SALE 이후: 예매된 좌석이 없는 경우에만 수정/삭제 가능\n\n" +
                            "**예시:**\n" +
                            "```json\n" +
                            "{\n" +
                            "  \"title\": \"레미제라블 (수정)\",\n" +
                            "  \"runningTime\": 160,\n" +
                            "  \"cast\": \"김철수, 이영희, 박민수\",\n" +
                            "  \"schedules\": [\n" +
                            "    {\n" +
                            "      \"scheduleId\": 1,\n" +
                            "      \"showDate\": \"2024-01-20\",\n" +
                            "      \"showTime\": \"19:00\",\n" +
                            "      \"ticketOpenTime\": \"2024-01-01T10:00:00\"\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"showDate\": \"2024-01-25\",\n" +
                            "      \"showTime\": \"19:00\",\n" +
                            "      \"ticketOpenTime\": \"2024-01-01T10:00:00\"\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}\n" +
                            "```", 
                required = true,
                schema = @Schema(implementation = ShowUpdateRequest.class),
                examples = {
                    @ExampleObject(
                        name = "제목 및 출연진 수정",
                        value = "{\"title\":\"레미제라블 (수정)\",\"cast\":\"김철수, 이영희, 박민수\"}"
                    ),
                    @ExampleObject(
                        name = "상영시간 수정",
                        value = "{\"runningTime\":160}"
                    ),
                    @ExampleObject(
                        name = "WAITING 상태 - 전체 수정",
                        value = "{\"title\":\"레미제라블\",\"genre\":\"MUSICAL\",\"location\":{\"region\":\"경기\",\"venue\":\"예술의전당\",\"hall\":\"A홀\"},\"saleStartDate\":\"2024-01-01T00:00:00\"}"
                    ),
                    @ExampleObject(
                        name = "일정 추가/수정/삭제",
                        value = "{\"schedules\":[{\"scheduleId\":1,\"showDate\":\"2024-01-20\",\"showTime\":\"19:00\",\"ticketOpenTime\":\"2024-01-01T10:00:00\"},{\"showDate\":\"2024-01-25\",\"showTime\":\"19:00\",\"ticketOpenTime\":\"2024-01-01T10:00:00\"}],\"deletedScheduleIds\":[2,3]}"
                    )
                }
            )
            @RequestPart("data") String dataJson,
            @Parameter(description = "포스터 이미지 파일 (선택, 새 파일 업로드 시에만)")
            @RequestPart(value = "poster", required = false) MultipartFile poster,
            @Parameter(description = "상세 이미지 파일 목록 (선택, 새 파일 업로드 시에만)")
            @RequestPart(value = "detailImages", required = false) List<MultipartFile> detailImages) throws Exception {
        
        // JSON 문자열을 DTO로 파싱 및 검증
        ShowUpdateRequest request = parseAndValidate(dataJson, ShowUpdateRequest.class);
        
        ShowUpdateResponse result = adminShowService.updateShow(id, request, poster, detailImages);
        return ResponseEntity.ok(ApiResponse.success(result, result.getMessage()));
    }

    @Operation(summary = "공연 삭제", description = "공연을 삭제합니다. WAITING 상태의 공연만 삭제 가능합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<ShowDeleteResponse>> deleteShow(
            @Parameter(description = "공연 ID", required = true) @PathVariable Long id) {
        ShowDeleteResponse result = adminShowService.deleteShow(id);
        return ResponseEntity.ok(ApiResponse.success(result, result.getMessage()));
    }

    // 일정 추가/수정/삭제는 공연 수정 API(PATCH /{id})에서 함께 처리됩니다.
    // 팝업에서 변경한 모든 일정을 schedules와 deletedScheduleIds에 포함하여 '완료' 버튼 클릭 시 최종 저장됩니다.

    @Operation(
            summary = "공연 판매 상태 변경",
            description = "공연의 판매 상태를 변경합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "상태 변경 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "성공 응답",
                                    value = "{\"success\":true,\"data\":{\"showId\":2,\"saleStatus\":\"SUSPENDED\",\"message\":\"판매 상태가 변경되었습니다\"},\"message\":\"판매 상태가 변경되었습니다\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "에러 응답",
                                    value = "{\"success\":false,\"data\":null,\"message\":\"유효하지 않은 판매 상태입니다\"}"
                            )
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "판매 상태 변경 요청",
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ShowSaleStatusUpdateRequest.class),
                    examples = {
                            @ExampleObject(
                                    name = "SUSPENDED 예시",
                                    value = "{\"saleStatus\": \"SUSPENDED\"}"
                            ),
                            @ExampleObject(
                                    name = "ALLOWED 예시",
                                    value = "{\"saleStatus\": \"ALLOWED\"}"
                            )
                    }
            )
    )
    @PatchMapping(value = "/{id}/sale-status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ShowSaleStatusUpdateResponse>> updateSaleStatus(
            @Parameter(description = "공연 ID", required = true) @PathVariable Long id,
            @RequestBody ShowSaleStatusUpdateRequest showSaleResponse) throws IOException {
        log.info("test  : {}",showSaleResponse.toString());
        log.info("=== 판매 상태 변경 ===");
        log.info("Show ID: {}", id);

        try {
            ShowSaleStatusUpdateResponse response = adminShowService.updateSaleStatus(id, showSaleResponse);
            return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));

        } catch (IllegalArgumentException e) {
            log.error("유효하지 않은 요청: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));

        } catch (RuntimeException e) {
            log.error("판매 상태 변경 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }




    /**
     * JSON 문자열을 파싱하고 유효성 검증을 수행하는 헬퍼 메서드
     */
    private <T> T parseAndValidate(String json, Class<T> clazz) {
        try {
            // JSON 파싱
            T dto = objectMapper.readValue(json, clazz);
            
            // 유효성 검증
            Set<ConstraintViolation<T>> violations = validator.validate(dto);
            if (!violations.isEmpty()) {
                String errorMessage = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));
                log.error("Validation 실패: {}", errorMessage);
                throw new IllegalArgumentException("Validation failed: " + errorMessage);
            }
            
            return dto;
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("JSON 파싱 실패: {}", e.getMessage());
            log.debug("JSON 내용: {}", json);
            throw new IllegalArgumentException("JSON 파싱 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("예상치 못한 오류: {}", e.getMessage(), e);
            throw new IllegalArgumentException("요청 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}

