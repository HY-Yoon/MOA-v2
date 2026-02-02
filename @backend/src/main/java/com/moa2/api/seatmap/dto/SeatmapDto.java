package com.moa2.api.seatmap.dto;

import com.moa2.global.model.Region;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 좌석배치도 관련 DTO 통합 클래스 (Record 변환 완료)
 */
public class SeatmapDto {

        // ===== Request DTOs =====

        /**
         * 좌석배치도 목록 조회 요청 DTO
         */
        @Builder
        @Schema(name = "SeatmapListRequest")
        public record ListRequest(
                        @Schema(description = "지역") Region region,

                        @Schema(description = "공연장명") String venueName,

                        @Schema(description = "홀명") String hallName,

                        @Schema(description = "페이지 번호", defaultValue = "0") Integer page,

                        @Schema(description = "페이지 크기", defaultValue = "20") Integer size) {
                // @ModelAttribute가 생성자를 호출할 때 이 로직이 실행되어 기본값이 들어감
                public ListRequest {
                        if (page == null)
                                page = 0;
                        if (size == null)
                                size = 20;
                }
        }

        /**
         * 좌석배치도 등록 요청 DTO
         */
        @Builder
        @Schema(name = "SeatmapCreateRequest", description = "좌석배치도 등록 요청")
        public record CreateRequest(
                        @Schema(description = "지역", requiredMode = Schema.RequiredMode.REQUIRED, example = "SEOUL") @NotNull(message = "지역은 필수입니다") Region region,
                        @Schema(description = "공연장명", requiredMode = Schema.RequiredMode.REQUIRED, example = "올림픽공원") @NotBlank(message = "공연장명은 필수입니다") String venueName,
                        @Schema(description = "홀명", requiredMode = Schema.RequiredMode.REQUIRED, example = "KSPO DOME") @NotBlank(message = "홀명은 필수입니다") String hallName,
                        @Schema(description = "캔버스 정보", requiredMode = Schema.RequiredMode.REQUIRED) @Valid @NotNull(message = "캔버스 정보는 필수입니다") CanvasRequest canvas,
                        @Schema(description = "구역 목록", requiredMode = Schema.RequiredMode.REQUIRED) @Valid @NotNull(message = "구역 목록은 필수입니다") List<SectionRequest> sections,
                        @Schema(description = "좌석 목록", requiredMode = Schema.RequiredMode.REQUIRED) @Valid @NotNull(message = "좌석 목록은 필수입니다") List<SeatRequest> seats) {
                @Builder
                @Schema(name = "SeatmapCanvasRequest")
                public record CanvasRequest(
                                @Schema(description = "너비", example = "1200") @NotNull(message = "너비는 필수입니다") Integer width,
                                @Schema(description = "높이", example = "800") @NotNull(message = "높이는 필수입니다") Integer height,
                                @Schema(description = "좌석 반지름", example = "10") @NotNull(message = "좌석 반지름은 필수입니다") Integer seatRadius,
                                @Schema(description = "행 간격", example = "20") Integer rowGap,
                                @Schema(description = "열 간격", example = "20") Integer columnGap) {
                }

                @Builder
                @Schema(name = "SeatmapSectionRequest")
                public record SectionRequest(
                                @Schema(description = "구역 ID", example = "A") @NotBlank(message = "구역 ID는 필수입니다") String sectionId,
                                @Schema(description = "구역명", example = "A구역") @NotBlank(message = "구역명은 필수입니다") String name,
                                @Schema(description = "색상", example = "#FF6B6B") @NotBlank(message = "색상은 필수입니다") String color,
                                @Schema(description = "가격", example = "150000") @NotNull(message = "가격은 필수입니다") Integer price) {
                }

                @Builder
                @Schema(name = "SeatmapSeatRequest")
                public record SeatRequest(
                                @Schema(description = "좌석 ID", example = "A-1") @NotBlank(message = "좌석 ID는 필수입니다") String seatId,
                                @Schema(description = "구역 ID", example = "A") @NotBlank(message = "구역 ID는 필수입니다") String sectionId,
                                @Schema(description = "행", example = "A") @NotBlank(message = "행은 필수입니다") String row,
                                @Schema(description = "번호", example = "1") @NotNull(message = "번호는 필수입니다") Integer number,
                                @Schema(description = "X 좌표", example = "100") @NotNull(message = "X 좌표는 필수입니다") Integer x,
                                @Schema(description = "Y 좌표", example = "100") @NotNull(message = "Y 좌표는 필수입니다") Integer y) {
                }
        }

        /**
         * 좌석배치도 중복 확인 요청 DTO
         */
        @Builder
        @Schema(name = "SeatmapDuplicateCheckRequest")
        public record DuplicateCheckRequest(
                        @Schema(description = "지역", example = "SEOUL") @NotNull(message = "지역은 필수입니다") // 검색 조건이지만 필수라면
                                                                                                       // 추가
                        Region region,

                        @Schema(description = "공연장명", example = "올림픽공원") @NotBlank(message = "공연장명은 필수입니다") String venueName,

                        @Schema(description = "홀명", example = "KSPO DOME") @NotBlank(message = "홀명은 필수입니다") String hallName) {
        }

        // ===== Response DTOs =====

        @Schema(name = "SeatmapCreateResponse")
        public record CreateResponse(String seatMapId) {
        }

        @Builder
        @Schema(name = "SeatmapListResponse")
        public record ListResponse(
                        String seatMapId,
                        String region,
                        String venueName,
                        String hallName,
                        LocalDateTime createdAt,
                        LocalDateTime updatedAt) {
        }

        @Schema(name = "SeatmapDuplicateCheckResponse")
        public record DuplicateCheckResponse(
                        @Schema(description = "중복 여부", example = "true") boolean isDuplicate) {
        }
}