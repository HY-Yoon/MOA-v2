package com.moa2.api.show.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.moa2.global.model.Genre;
import com.moa2.global.model.Region;
import com.moa2.global.model.SaleStatus;
import com.moa2.global.model.ShowStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 공연 관련 DTO 통합 클래스 (Record 변환 완료)
 */
public class ShowDto {

    // ===== Request DTOs =====

    /**
     * 공연 목록 조회 요청 DTO (사용자용 + 관리자용)
     * (검색 필터는 필드가 많으므로 Builder 패턴 적용)
     */
    @Builder
    public record ListRequest(
            // 사용자용 필터
            Genre genre,
            Region region,

            // 공통 필터
            String keyword,
            LocalDate startDate,
            LocalDate endDate,

            // 관리자용 필터
            ShowStatus showStatus,
            SaleStatus saleStatus,

            // 정렬 및 페이지네이션 (기본값 처리는 서비스 계층이나 커스텀 생성자에서)
            String orderBy,
            String orderDirection,
            String sort,
            Integer page,
            Integer size
    ) {
        // 기본값 설정을 위한 Compact Constructor
        public ListRequest {
            if (page == null) page = 0;
            if (size == null) size = 20;
            if (orderBy == null) orderBy = "createdAt";
            if (orderDirection == null) orderDirection = "desc";
        }
    }

    /**
     * 공연 등록 요청 DTO
     */
    @Builder
    @Schema(description = "공연 등록 요청")
    public record CreateRequest(
            @Schema(description = "공연 제목", example = "레미제라블", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "제목은 필수입니다")
            String title,

            @Schema(description = "장르", example = "MUSICAL", allowableValues = {"MUSICAL", "CONCERT", "PLAY", "CLASSIC", "DANCE"}, requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull(message = "장르는 필수입니다")
            String genre,

            @Schema(description = "장소 정보", requiredMode = Schema.RequiredMode.REQUIRED)
            @Valid
            @NotNull(message = "장소 정보는 필수입니다")
            LocationRequest location,

            @Schema(description = "상영 시간", example = "150분", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "상영 시간은 필수입니다")
            String runningTime,

            @Schema(description = "출연진 정보 (문자열)", example = "출연진1, 출연진2, 출연진3")
            String cast,

            @Schema(description = "판매 기간", requiredMode = Schema.RequiredMode.REQUIRED)
            @Valid
            @NotNull(message = "판매 기간은 필수입니다")
            SalePeriodRequest salePeriod,

            @Schema(description = "공연 스케줄 목록", requiredMode = Schema.RequiredMode.REQUIRED)
            @Valid
            @NotEmpty(message = "스케줄은 최소 1개 이상 필요합니다")
            List<ScheduleRequest> schedules
    ) {
        @Builder
        @Schema(description = "장소 정보")
        public record LocationRequest(
                @Schema(description = "지역", example = "SEOUL", requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "지역은 필수입니다")
                Region region,

                @Schema(description = "공연장명", example = "올림픽공원", requiredMode = Schema.RequiredMode.REQUIRED)
                @NotBlank(message = "공연장명은 필수입니다")
                String venueName,

                @Schema(description = "홀명", example = "KSPO DOME", requiredMode = Schema.RequiredMode.REQUIRED)
                @NotBlank(message = "홀명은 필수입니다")
                String hallName
        ) {}

        @Builder
        @Schema(description = "판매 기간")
        public record SalePeriodRequest(
                @Schema(description = "판매 시작일시", example = "2026-01-01T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "판매 시작일시는 필수입니다")
                LocalDateTime startDate,

                @Schema(description = "판매 종료일시", example = "2026-01-31T23:59:59", requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "판매 종료일시는 필수입니다")
                LocalDateTime endDate
        ) {}

        @Builder
        @Schema(description = "공연 스케줄 정보")
        public record ScheduleRequest(
                @Schema(description = "공연일", example = "2026-01-15", requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "공연일은 필수입니다")
                LocalDate showDate,

                @Schema(description = "공연 시간", example = "19:00", requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "공연 시간은 필수입니다")
                String showTime,

                @Schema(description = "티켓 오픈 시간", example = "2026-01-01T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "티켓 오픈 시간은 필수입니다")
                LocalDateTime ticketOpenTime
        ) {}
    }

    /**
     * 공연 수정 요청 DTO
     */
    @Builder
    @Schema(description = "공연 수정 요청")
    public record UpdateRequest(
            @Schema(description = "공연 제목", example = "레미제라블")
            String title,

            @Schema(description = "장르 (ON_SALE 이후 수정 불가)", example = "MUSICAL")
            String genre,

            @Schema(description = "장소 정보 (ON_SALE 이후 수정 불가)")
            @Valid
            LocationRequest location,

            @Schema(description = "상영 시간", example = "150분")
            String runningTime,

            @Schema(description = "출연진 정보 (문자열)", example = "출연진1, 출연진2, 출연진3")
            String cast,

            @Schema(description = "판매 기간 (ON_SALE 이후 수정 불가)")
            @Valid
            SalePeriodRequest salePeriod,

            @Schema(description = "공연 스케줄 목록 (추가/수정/삭제 모두 포함)")
            @Valid
            List<ScheduleUpdateRequest> schedules,

            @Schema(description = "삭제할 스케줄 ID 목록", example = "[2, 3]")
            List<Long> deletedScheduleIds,

            @Schema(description = "삭제할 상세 이미지 ID 목록", example = "[1, 3, 5]")
            List<Long> deletedDetailImageIds
    ) {
        @Builder
        @Schema(description = "장소 정보")
        public record LocationRequest(
                @Schema(description = "지역", example = "SEOUL")
                Region region,

                @Schema(description = "공연장명", example = "올림픽공원")
                String venueName,

                @Schema(description = "홀명", example = "KSPO DOME")
                String hallName
        ) {}

        @Builder
        @Schema(description = "판매 기간")
        public record SalePeriodRequest(
                @Schema(description = "판매 시작일시", example = "2026-01-01T10:00:00")
                LocalDateTime startDate,

                @Schema(description = "판매 종료일시", example = "2026-01-31T23:59:59")
                LocalDateTime endDate
        ) {}

        @Builder
        @Schema(description = "공연 스케줄 정보")
        public record ScheduleUpdateRequest(
                @Schema(description = "스케줄 ID (수정 시 필수, 추가 시 null)", example = "1")
                Long scheduleId,

                @Schema(description = "공연일", example = "2026-01-15")
                LocalDate showDate,

                @Schema(description = "공연 시간", example = "19:00")
                String showTime,

                @Schema(description = "티켓 오픈 시간", example = "2026-01-01T10:00:00")
                LocalDateTime ticketOpenTime
        ) {}
    }

    /**
     * 판매 상태 변경 요청 DTO
     * (필드 1개 -> Builder 불필요)
     */
    @Schema(description = "판매 상태 변경 요청")
    public record SaleStatusUpdateRequest(
            @Schema(description = "판매 상태", example = "SUSPENDED", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"ALLOWED", "SUSPENDED"})
            @NotNull(message = "판매 상태는 필수입니다")
            SaleStatus saleStatus
    ) {}

    // ===== Response DTOs =====

    /**
     * 공연 목록 조회 응답 DTO
     */
    @Builder
    @Schema(description = "공연 목록 조회 응답")
    public record ListResponse(
            @Schema(description = "공연 ID", example = "1")
            Long id,

            @Schema(description = "공연 제목", example = "레미제라블")
            String title,

            @Schema(description = "장르", example = "MUSICAL")
            String genre,

            @Schema(description = "공연 상태", example = "ON_SALE")
            String status,

            @Schema(description = "포스터 URL", example = "/images/posters/show1.jpg")
            String posterUrl,

            @Schema(description = "공연 장소 정보")
            LocationInfo location,

            @Schema(description = "판매 기간")
            SalePeriod salePeriod,

            @Schema(description = "공연 일정 목록")
            List<ScheduleInfo> schedules
    ) {
        @Builder
        @Schema(description = "공연 장소 정보")
        public record LocationInfo(
                @Schema(description = "지역", example = "SEOUL")
                String region,

                @Schema(description = "공연장명", example = "예술의전당")
                String venue,

                @Schema(description = "홀명", example = "오페라극장")
                String hallName
        ) {}

        @Builder
        @Schema(description = "판매 기간 정보")
        public record SalePeriod(
                @Schema(description = "판매 시작일시", example = "2025-01-01T10:00:00")
                LocalDateTime startDate,

                @Schema(description = "판매 종료일시", example = "2025-12-31T23:59:59")
                LocalDateTime endDate
        ) {}

        @Builder
        @Schema(description = "공연 일정 정보")
        public record ScheduleInfo(
                @Schema(description = "일정 ID", example = "1")
                Long keyId,

                @Schema(description = "공연일", example = "2025-02-20")
                @JsonFormat(pattern = "yyyy-MM-dd")
                LocalDate date,

                @Schema(description = "공연 시간", example = "19:00")
                @JsonFormat(pattern = "HH:mm")
                LocalTime time,

                @Schema(description = "회차", example = "1")
                Integer session
        ) {}
    }

    /**
     * 공연 상세 조회 응답 DTO (사용자용)
     */
    @Builder
    @Schema(description = "공연 상세 조회 응답")
    public record DetailResponse(
            @Schema(description = "공연 ID", example = "1")
            Long id,

            @Schema(description = "공연 제목", example = "레미제라블")
            String title,

            @Schema(description = "장르", example = "MUSICAL")
            String genre,

            @Schema(description = "공연 상태", example = "ON_SALE")
            String status,

            @Schema(description = "포스터 URL", example = "/images/posters/show1.jpg")
            String posterUrl,

            @Schema(description = "상세 이미지 URL 목록")
            List<String> detailImageUrls,

            @Schema(description = "공연 장소 정보")
            LocationInfo location,

            @Schema(description = "상영 시간", example = "150분")
            String runningTime,

            @Schema(description = "출연진 정보", example = "김철수, 이영희, 박민수")
            String cast,

            @Schema(description = "판매 기간")
            SalePeriod salePeriod,

            @Schema(description = "공연 일정 목록")
            List<ScheduleInfo> schedules,

            @Schema(description = "좌석 가격 정보 목록")
            List<SeatGradeInfo> seatGrades
    ) {
        @Builder
        @Schema(description = "공연 장소 정보")
        public record LocationInfo(
                @Schema(description = "지역", example = "SEOUL")
                String region,

                @Schema(description = "공연장명", example = "예술의전당")
                String venue,

                @Schema(description = "홀명", example = "오페라극장")
                String hallName
        ) {}

        @Builder
        @Schema(description = "판매 기간 정보")
        public record SalePeriod(
                @Schema(description = "판매 시작일시", example = "2025-01-01T10:00:00")
                LocalDateTime startDate,

                @Schema(description = "판매 종료일시", example = "2025-12-31T23:59:59")
                LocalDateTime endDate
        ) {}

        @Builder
        @Schema(description = "공연 일정 정보")
        public record ScheduleInfo(
                @Schema(description = "스케줄 ID", example = "1")
                Long scheduleId,

                @Schema(description = "공연일", example = "2025-02-20")
                @JsonFormat(pattern = "yyyy-MM-dd")
                LocalDate showDate,

                @Schema(description = "공연 시간", example = "19:00")
                @JsonFormat(pattern = "HH:mm")
                LocalTime showTime,

                @Schema(description = "티켓 오픈 시간", example = "2025-01-10T10:00:00")
                LocalDateTime ticketOpenTime,

                @Schema(description = "남은 좌석 수", example = "150")
                Integer remainingSeats,

                @Schema(description = "전체 좌석 수", example = "2000")
                Integer totalSeats
        ) {}

        @Builder
        @Schema(description = "좌석 가격 정보")
        public record SeatGradeInfo(
                @Schema(description = "구역 ID", example = "1")
                String sectionId,

                @Schema(description = "구역명", example = "VIP석")
                String sectionName,

                @Schema(description = "가격", example = "150000")
                Integer price
        ) {}
    }

    /**
     * 공연 상세 조회 응답 DTO (관리자용)
     */
    @Builder
    @Schema(description = "관리자용 공연 상세 조회 응답")
    public record AdminDetailResponse(
            @Schema(description = "공연 ID", example = "1")
            Long id,

            @Schema(description = "공연 제목", example = "레미제라블")
            String title,

            @Schema(description = "장르", example = "MUSICAL")
            String genre,

            @Schema(description = "공연 상태", example = "ON_SALE")
            String status,

            @Schema(description = "판매 상태", example = "ALLOWED")
            String saleStatus,

            @Schema(description = "포스터 URL", example = "/images/posters/show1.jpg")
            String posterUrl,

            @Schema(description = "상영 시간", example = "150분")
            String runningTime,

            @Schema(description = "출연진 정보", example = "김철수, 이영희, 박민수")
            String cast,

            @Schema(description = "공연장명", example = "예술의전당")
            String venueName,

            @Schema(description = "홀명", example = "오페라극장")
            String hallName,

            @Schema(description = "지역", example = "SEOUL")
            String region,

            @Schema(description = "판매 시작일시", example = "2025-01-01T10:00:00")
            LocalDateTime saleStartDate,

            @Schema(description = "판매 종료일시", example = "2025-12-31T23:59:59")
            LocalDateTime saleEndDate,

            @Schema(description = "공연 일정 목록")
            List<AdminScheduleInfo> schedules,

            @Schema(description = "좌석 가격 정보 목록")
            List<SeatPriceInfo> seatPrices,

            @Schema(description = "상세 이미지 목록")
            List<DetailImageInfo> detailImages,

            @Schema(description = "생성일시")
            LocalDateTime createdAt,

            @Schema(description = "수정일시")
            LocalDateTime updatedAt
    ) {
        @Builder
        @Schema(description = "공연 일정 정보 (관리자용)")
        public record AdminScheduleInfo(
                @Schema(description = "스케줄 ID", example = "1")
                Long scheduleId,

                @Schema(description = "공연일", example = "2025-02-20")
                @JsonFormat(pattern = "yyyy-MM-dd")
                LocalDate showDate,

                @Schema(description = "공연 시간", example = "19:00")
                @JsonFormat(pattern = "HH:mm")
                LocalTime showTime,

                @Schema(description = "티켓 오픈 시간", example = "2025-01-10T10:00:00")
                LocalDateTime ticketOpenTime,

                @Schema(description = "남은 좌석 수", example = "150")
                Integer remainingSeats,

                @Schema(description = "전체 좌석 수", example = "2000")
                Integer totalSeats,

                @Schema(description = "예매된 좌석 수", example = "1850")
                Integer reservationCount
        ) {}

        @Builder
        @Schema(description = "좌석 가격 정보 (관리자용)")
        public record SeatPriceInfo(
                @Schema(description = "구역 ID", example = "1")
                String sectionId,

                @Schema(description = "구역명", example = "VIP석")
                String sectionName,

                @Schema(description = "가격", example = "150000")
                Integer price
        ) {}

        @Builder
        @Schema(description = "상세 이미지 정보")
        public record DetailImageInfo(
                @Schema(description = "이미지 ID", example = "1")
                Long id,

                @Schema(description = "이미지 URL", example = "/images/details/show1-1.jpg")
                String url
        ) {}
    }

    /**
     * 공연 등록 응답 DTO
     */
    public record CreateResponse(
            Long showId,
            String message
    ) {}

    /**
     * 공연 수정 응답 DTO
     */
    public record UpdateResponse(
            Long showId,
            String message
    ) {}

    /**
     * 공연 삭제 응답 DTO
     */
    public record DeleteResponse(
            String message
    ) {}

    /**
     * 판매 상태 변경 응답 DTO
     */
    public record SaleStatusUpdateResponse(
            Long showId,
            SaleStatus saleStatus,
            String message
    ) {}

    /**
     * 공연 회차 조회 응답 DTO
     */
    public record ScheduleListResponse(
            Long scheduleId,
            LocalDate date,
            LocalTime time,
            boolean isSoldOut
    ) {}

    /**
     * 스케줄별 잔여석 조회 응답 DTO
     */
    @Builder
    @Schema(description = "스케줄별 잔여석 조회 응답")
    public record SeatAvailabilityResponse(
            @Schema(description = "스케줄 ID", example = "1")
            Long scheduleId,

            @Schema(description = "공연 ID", example = "1")
            Long showId,

            @Schema(description = "공연일", example = "2025-02-20")
            @JsonFormat(pattern = "yyyy-MM-dd")
            LocalDate showDate,

            @Schema(description = "공연 시간", example = "19:00")
            @JsonFormat(pattern = "HH:mm")
            LocalTime showTime,

            @Schema(description = "구역별 좌석 가용성 정보")
            List<SeatAvailability> seatAvailability,

            @Schema(description = "전체 좌석 수", example = "2000")
            Integer totalSeats,

            @Schema(description = "전체 잔여석 수", example = "1675")
            Integer totalRemainingSeats,

            @Schema(description = "전체 가용률 (%)", example = "84")
            Integer totalAvailabilityRate
    ) {
        @Builder
        @Schema(description = "구역별 좌석 가용성")
        public record SeatAvailability(
                @Schema(description = "구역 ID", example = "1")
                String sectionId,

                @Schema(description = "구역명", example = "VIP석")
                String sectionName,

                @Schema(description = "가격", example = "150000")
                Integer price,

                @Schema(description = "전체 좌석 수", example = "100")
                Integer totalSeats,

                @Schema(description = "잔여석 수", example = "45")
                Integer remainingSeats,

                @Schema(description = "가용률 (%)", example = "45")
                Integer availabilityRate
        ) {}
    }
}