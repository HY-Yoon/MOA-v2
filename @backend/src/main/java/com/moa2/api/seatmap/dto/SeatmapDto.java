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
    public record ListRequest(
            Region region,
            String venueName,
            String hallName,
            Integer page,
            Integer size
    ) {
        // 기본값 설정을 위한 생성자
        public ListRequest {
            if (page == null) page = 0;
            if (size == null) size = 20;
        }
    }

    /**
     * 좌석배치도 등록 요청 DTO
     */
    @Builder
    @Schema(description = "좌석배치도 등록 요청")
    public record CreateRequest(
            @Schema(description = "지역", example = "SEOUL", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull(message = "지역은 필수입니다")
            Region region,

            @Schema(description = "공연장명", example = "올림픽공원", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "공연장명은 필수입니다")
            String venueName,

            @Schema(description = "홀명", example = "KSPO DOME", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "홀명은 필수입니다")
            String hallName,

            @Schema(description = "캔버스 정보", requiredMode = Schema.RequiredMode.REQUIRED)
            @Valid @NotNull(message = "캔버스 정보는 필수입니다")
            CanvasRequest canvas,

            @Schema(description = "구역 목록", requiredMode = Schema.RequiredMode.REQUIRED)
            @Valid @NotNull(message = "구역 목록은 필수입니다")
            List<SectionRequest> sections,

            @Schema(description = "좌석 목록", requiredMode = Schema.RequiredMode.REQUIRED)
            @Valid @NotNull(message = "좌석 목록은 필수입니다")
            List<SeatRequest> seats
    ) {
        @Builder
        public record CanvasRequest(
                @NotNull(message = "너비는 필수입니다") Integer width,
                @NotNull(message = "높이는 필수입니다") Integer height,
                @NotNull(message = "좌석 반지름은 필수입니다") Integer seatRadius,
                @NotNull(message = "행 간격은 필수입니다") Integer rowGap,
                @NotNull(message = "열 간격은 필수입니다") Integer columnGap
        ) {}

        @Builder
        public record SectionRequest(
                @NotBlank(message = "구역 ID는 필수입니다") String sectionId,
                @NotBlank(message = "구역명은 필수입니다") String name,
                @NotBlank(message = "색상은 필수입니다") String color,
                @NotNull(message = "가격은 필수입니다") Integer price
        ) {}

        @Builder
        public record SeatRequest(
                @NotBlank(message = "좌석 ID는 필수입니다") String seatId,
                @NotBlank(message = "구역 ID는 필수입니다") String sectionId,
                @NotBlank(message = "행은 필수입니다") String row,
                @NotNull(message = "번호는 필수입니다") Integer number,
                @NotNull(message = "X 좌표는 필수입니다") Integer x,
                @NotNull(message = "Y 좌표는 필수입니다") Integer y
        ) {}
    }

    /**
     * 좌석배치도 중복 확인 요청 DTO
     */
    @Builder
    public record DuplicateCheckRequest(
            Region region,
            String venueName,
            String hallName
    ) {}

    // ===== Response DTOs =====

    public record CreateResponse(String seatMapId) {}

    @Builder
    public record ListResponse(
            String seatMapId,
            String region,
            String venueName,
            String hallName,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record DuplicateCheckResponse(boolean isDuplicate) {}
}