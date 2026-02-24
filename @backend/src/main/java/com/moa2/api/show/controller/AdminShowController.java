package com.moa2.api.show.controller;

import com.moa2.api.show.controller.docs.AdminShowControllerDocs;
import com.moa2.global.util.JsonDataParser;
import com.moa2.api.show.service.AdminShowService;
import com.moa2.api.show.dto.*;
import com.moa2.global.dto.ApiResponse;
import com.moa2.global.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/shows")
@RequiredArgsConstructor
public class AdminShowController implements AdminShowControllerDocs {

        private final AdminShowService adminShowService;
        private final JsonDataParser jsonDataParser;

        // Constants for Sort
        private static final String DEFAULT_SORT_FIELD = "id";
        private static final String DEFAULT_SORT_DIRECTION = "desc";
        private static final String SORT_SCHEDULE = "schedule";
        private static final String SORT_SALE_PERIOD = "salePeriod";
        private static final String SORT_SEPARATOR = ",";

        @Override
        @GetMapping
        public ResponseEntity<ApiResponse<PageResponse<ShowDto.AdminListResponse>>> getShowList(
                        @ParameterObject @ModelAttribute ShowDto.AdminListRequest request) {

                Sort finalSort = createSort(request.sort());
                int page = request.page() != null ? request.page() : 0;
                int size = request.size() != null ? request.size() : 20;

                Pageable pageable = PageRequest.of(page, size, finalSort);

                Page<ShowDto.AdminListResponse> pageResult = adminShowService.getShowList(request, pageable);
                PageResponse<ShowDto.AdminListResponse> result = PageResponse.of(pageResult);
                return ResponseEntity.ok(ApiResponse.success(result));
        }

        private Sort createSort(String sortParam) {
                if (sortParam == null || sortParam.isBlank()) {
                        return Sort.by(Sort.Direction.fromString(DEFAULT_SORT_DIRECTION), DEFAULT_SORT_FIELD);
                }

                String[] sortParts = sortParam.split(SORT_SEPARATOR);
                String sortField = sortParts[0].trim();
                Sort.Direction direction = sortParts.length > 1
                                && DEFAULT_SORT_DIRECTION.equalsIgnoreCase(sortParts[1].trim())
                                                ? Sort.Direction.DESC
                                                : Sort.Direction.ASC;

                if (SORT_SCHEDULE.equals(sortField)) {
                        // 일정 정렬: StartDate -> StartTime (둘 다 Show 엔티티에 있음)
                        return Sort.by(direction, "startDate", "startTime");
                } else if (SORT_SALE_PERIOD.equals(sortField)) {
                        // 예매 일정 정렬: SaleStartDate
                        return Sort.by(direction, "saleStartDate");
                } else {
                        // 그 외 필드 (id, title, status 등)
                        return Sort.by(direction, sortField);
                }
        }

        @Override
        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<ShowDto.AdminDetailResponse>> getShowDetail(
                        @PathVariable Long id) {
                ShowDto.AdminDetailResponse result = adminShowService.getShowDetail(id);
                return ResponseEntity.ok(ApiResponse.success(result));
        }

        @Override
        @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<ApiResponse<ShowDto.CreateResponse>> createShow(
                        @RequestPart("data") String dataJson,
                        @RequestPart("poster") MultipartFile poster,
                        @RequestPart(value = "detailImages", required = false) List<MultipartFile> detailImages) {

                ShowDto.CreateRequest request = jsonDataParser.parseAndValidate(dataJson, ShowDto.CreateRequest.class);

                ShowDto.CreateResponse result = adminShowService.createShow(request, poster, detailImages);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success(result, result.message()));
        }

        @Override
        @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<ApiResponse<ShowDto.UpdateResponse>> updateShow(
                        @PathVariable Long id,
                        @RequestPart("data") String dataJson,
                        @RequestPart(value = "poster", required = false) MultipartFile poster,
                        @RequestPart(value = "detailImages", required = false) List<MultipartFile> detailImages)
                        throws Exception {

                ShowDto.UpdateRequest request = jsonDataParser.parseAndValidate(dataJson, ShowDto.UpdateRequest.class);

                ShowDto.UpdateResponse result = adminShowService.updateShow(id, request, poster, detailImages);
                return ResponseEntity.ok(ApiResponse.success(result, result.message()));
        }

        @Override
        @DeleteMapping("/{id}")
        public ResponseEntity<ApiResponse<ShowDto.DeleteResponse>> deleteShow(
                        @PathVariable Long id) {
                ShowDto.DeleteResponse result = adminShowService.deleteShow(id);
                return ResponseEntity.ok(ApiResponse.success(result, result.message()));
        }

        @Override
        @PatchMapping(value = "/{id}/sale-status", consumes = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<ApiResponse<ShowDto.SaleStatusUpdateResponse>> updateSaleStatus(
                        @PathVariable Long id,
                        @RequestBody ShowDto.SaleStatusUpdateRequest showSaleResponse) {

                ShowDto.SaleStatusUpdateResponse response = adminShowService.updateSaleStatus(id, showSaleResponse);
                return ResponseEntity.ok(ApiResponse.success(response, response.message()));
        }
}
