package com.moa2.api.show.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.moa2.global.model.Genre;
import com.moa2.global.model.Region;
import com.moa2.global.model.SaleStatus;
import com.moa2.global.model.ShowStatus;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.moa2.api.show.domain.entity.Show;
import com.moa2.api.show.domain.entity.ShowSchedule;
import com.moa2.api.show.domain.entity.ShowSeatGrade;

/**
 * 공연 관련 DTO 통합 클래스
 */
public class ShowDto {

        // ===== Helper Methods =====
        public static ListResponse from(Show show, List<ShowSchedule> schedules) {
                // 회차 계산 (날짜별 1회차, 2회차...)
                Map<LocalDate, Integer> sessionCountByDate = new HashMap<>();
                List<ListResponse.ScheduleInfo> scheduleInfos = schedules.stream()
                                .map(schedule -> {
                                        LocalDate date = schedule.getShowDate();
                                        int session = sessionCountByDate.getOrDefault(date, 0) + 1;
                                        sessionCountByDate.put(date, session);

                                        return ListResponse.ScheduleInfo.builder()
                                                        .keyId(schedule.getId())
                                                        .date(schedule.getShowDate())
                                                        .time(schedule.getShowTime())
                                                        .session(session)
                                                        .build();
                                })
                                .collect(Collectors.toList());

                return ListResponse.builder()
                                .id(show.getId())
                                .title(show.getTitle())
                                .genre(show.getGenre() != null ? show.getGenre().name() : null)
                                .status(show.getStatus() != null ? show.getStatus().name() : null)
                                .saleStatus(show.getSaleStatus() != null ? show.getSaleStatus().name() : null)
                                .posterUrl(show.getPosterUrl())
                                .location(mapToListLocationInfo(show))
                                .salePeriod(mapToListSalePeriod(show))
                                .createdAt(show.getCreatedAt())
                                .viewCount(show.getViewCount())
                                .startDate(show.getStartDate())
                                .endDate(show.getEndDate())
                                .schedules(scheduleInfos)
                                .build();
        }

        public static DetailResponse of(Show show, List<ShowSchedule> schedules) {
                // 날짜별 회차 카운팅
                Map<LocalDate, Integer> sessionCountByDate = new HashMap<>();
                List<DetailResponse.ScheduleInfo> scheduleInfos = schedules.stream()
                                .map(schedule -> {
                                        LocalDate date = schedule.getShowDate();
                                        int session = sessionCountByDate.getOrDefault(date, 0) + 1;
                                        sessionCountByDate.put(date, session);

                                        return DetailResponse.ScheduleInfo.builder()
                                                        .keyId(schedule.getId())
                                                        .date(schedule.getShowDate())
                                                        .time(schedule.getShowTime())
                                                        .session(session)
                                                        .build();
                                })
                                .collect(Collectors.toList());

                List<String> detailImageUrls = show.getDetailImages().stream()
                                .map(com.moa2.api.show.domain.entity.DetailImage::getUrl)
                                .collect(Collectors.toList());

                return DetailResponse.builder()
                                .id(show.getId())
                                .title(show.getTitle())
                                .genre(show.getGenre() != null ? show.getGenre().name() : null)
                                .status(show.getStatus() != null ? show.getStatus().name() : null)
                                .posterUrl(show.getPosterUrl())
                                .detailImageUrls(detailImageUrls)
                                .location(mapToDetailLocationInfo(show))
                                .runningTime(show.getRunningTime())
                                .cast(show.getCast())
                                .salePeriod(mapToDetailSalePeriod(show))
                                .schedules(scheduleInfos)
                                .serverCurrentTime(LocalDateTime.now())
                                .build();
        }

        public static AdminListResponse fromAdmin(Show show, List<ShowSchedule> schedules) {
                Map<LocalDate, Integer> sessionCountByDate = new HashMap<>();
                List<AdminListResponse.ScheduleInfo> scheduleInfos = schedules.stream()
                                .map(schedule -> {
                                        LocalDate date = schedule.getShowDate();
                                        int session = sessionCountByDate.getOrDefault(date, 0) + 1;
                                        sessionCountByDate.put(date, session);

                                        return AdminListResponse.ScheduleInfo.builder()
                                                        .keyId(schedule.getId())
                                                        .date(schedule.getShowDate())
                                                        .time(schedule.getShowTime())
                                                        .session(session)
                                                        .build();
                                })
                                .collect(Collectors.toList());

                // 판매 기간 생성
                AdminListResponse.SalePeriod salePeriod = null;
                if (show.getSaleStartDate() != null || show.getSaleEndDate() != null) {
                        salePeriod = AdminListResponse.SalePeriod.builder()
                                        .startDate(show.getSaleStartDate())
                                        .endDate(show.getSaleEndDate())
                                        .build();
                }

                AdminListResponse.LocationInfo locationInfo = show.getVenue() != null
                                ? AdminListResponse.LocationInfo.builder()
                                                .region(show.getVenue().getRegion() != null
                                                                ? show.getVenue().getRegion().name()
                                                                : null)
                                                .venue(show.getVenue().getName())
                                                .hallName(show.getVenue().getHallName())
                                                .build()
                                : null;

                return AdminListResponse.builder()
                                .id(show.getId())
                                .title(show.getTitle())
                                .genre(show.getGenre() != null ? show.getGenre().name() : null)
                                .status(show.getStatus() != null ? show.getStatus().name() : null)
                                .saleStatus(show.getSaleStatus() != null ? show.getSaleStatus().name() : null)
                                .posterUrl(show.getPosterUrl())
                                .location(locationInfo)
                                .salePeriod(salePeriod)
                                .createdAt(show.getCreatedAt())
                                .schedules(scheduleInfos)
                                .build();
        }

        public static AdminDetailResponse ofAdminDetail(Show show, List<ShowSchedule> schedules,
                        List<ShowSeatGrade> seatGrades, Long totalSeats, Map<Long, Long> reservationCounts) {
                // 스케줄 정보 구성
                List<AdminDetailResponse.AdminScheduleInfo> scheduleInfos = schedules.stream()
                                .map(schedule -> {
                                        Long reservationCount = reservationCounts.getOrDefault(schedule.getId(), 0L);
                                        Long remainingSeats = totalSeats - reservationCount;

                                        return AdminDetailResponse.AdminScheduleInfo.builder()
                                                        .scheduleId(schedule.getId())
                                                        .showDate(schedule.getShowDate())
                                                        .showTime(schedule.getShowTime())
                                                        .ticketOpenTime(schedule.getTicketOpenTime())
                                                        .remainingSeats(remainingSeats.intValue())
                                                        .totalSeats(totalSeats.intValue())
                                                        .reservationCount(reservationCount.intValue())
                                                        .build();
                                })
                                .collect(Collectors.toList());

                // 좌석 가격 정보 구성
                List<AdminDetailResponse.SeatPriceInfo> seatPriceInfos = seatGrades.stream()
                                .map(grade -> AdminDetailResponse.SeatPriceInfo.builder()
                                                .sectionId(grade.getSection().getId().toString())
                                                .sectionName(grade.getSection().getName())
                                                .price(grade.getPrice())
                                                .build())
                                .collect(Collectors.toList());

                // Venue에서 hallName 가져오기
                String hallName = show.getVenue() != null ? show.getVenue().getHallName() : null;

                // 상세 이미지 정보 구성
                List<AdminDetailResponse.DetailImageInfo> detailImageInfos = show.getDetailImages().stream()
                                .map(detailImage -> AdminDetailResponse.DetailImageInfo.builder()
                                                .id(detailImage.getId())
                                                .url(detailImage.getUrl())
                                                .build())
                                .collect(Collectors.toList());

                return AdminDetailResponse.builder()
                                .id(show.getId())
                                .title(show.getTitle())
                                .genre(show.getGenre() != null ? show.getGenre().name() : null)
                                .venueName(show.getVenue() != null ? show.getVenue().getName() : null)
                                .hallName(hallName)
                                .region(show.getVenue() != null && show.getVenue().getRegion() != null
                                                ? show.getVenue().getRegion().name()
                                                : null)
                                .runningTime(show.getRunningTime())
                                .posterUrl(show.getPosterUrl())
                                .detailImages(detailImageInfos)
                                .cast(show.getCast())
                                .status(show.getStatus() != null ? show.getStatus().name() : null)
                                .saleStatus(show.getSaleStatus() != null ? show.getSaleStatus().name() : null)
                                .saleStartDate(show.getSaleStartDate())
                                .saleEndDate(show.getSaleEndDate())
                                .schedules(scheduleInfos)
                                .seatPrices(seatPriceInfos)
                                .createdAt(show.getCreatedAt())
                                .updatedAt(show.getUpdatedAt())
                                .build();
        }

        private static ListResponse.LocationInfo mapToListLocationInfo(Show show) {
                if (show.getVenue() == null)
                        return null;
                return ListResponse.LocationInfo.builder()
                                .region(show.getVenue().getRegion() != null ? show.getVenue().getRegion().name() : null)
                                .venue(show.getVenue().getName())
                                .hallName(show.getVenue().getHallName())
                                .build();
        }

        private static ListResponse.SalePeriod mapToListSalePeriod(Show show) {
                if (show.getSaleStartDate() == null && show.getSaleEndDate() == null)
                        return null;
                return ListResponse.SalePeriod.builder()
                                .startDate(show.getSaleStartDate())
                                .endDate(show.getSaleEndDate())
                                .build();
        }

        private static DetailResponse.LocationInfo mapToDetailLocationInfo(Show show) {
                if (show.getVenue() == null)
                        return null;
                return DetailResponse.LocationInfo.builder()
                                .region(show.getVenue().getRegion() != null ? show.getVenue().getRegion().name() : null)
                                .venue(show.getVenue().getName())
                                .hallName(show.getVenue().getHallName())
                                .build();
        }

        private static DetailResponse.SalePeriod mapToDetailSalePeriod(Show show) {
                if (show.getSaleStartDate() == null && show.getSaleEndDate() == null)
                        return null;
                return DetailResponse.SalePeriod.builder()
                                .startDate(show.getSaleStartDate())
                                .endDate(show.getSaleEndDate())
                                .build();
        }

        // ===== Request DTOs =====

        @Builder
        @Schema(description = "공연 목록 조회 요청 (검색 필터)")
        public record ListRequest(
                        @Parameter(description = "장르") Genre genre,
                        @Parameter(description = "지역") Region region,
                        @Parameter(description = "검색 키워드 (공연 제목)") String keyword,
                        @Parameter(description = "공연 시작일") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                        @Parameter(description = "공연 종료일") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
                        @Parameter(description = "정렬 기준") String orderBy,
                        @Parameter(description = "정렬 방향 (ASC/DESC)") String orderDirection,
                        @Parameter(description = "페이지 번호") Integer page,
                        @Parameter(description = "페이지 크기") Integer size) {
                public ListRequest {
                        if (page == null)
                                page = 0;
                        if (size == null)
                                size = 20;
                        if (orderBy == null)
                                orderBy = "createdAt";
                        if (orderDirection == null)
                                orderDirection = "desc";
                }
        }

        @Builder
        @Schema(description = "관리자용 공연 목록 조회 요청")
        public record AdminListRequest(
                        @Parameter(description = "장르") Genre genre,
                        @Parameter(description = "지역") Region region,
                        @Parameter(description = "검색 키워드 (공연 제목)") String keyword,
                        @Parameter(description = "공연 시작일") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                        @Parameter(description = "공연 종료일") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
                        @Parameter(description = "공연 상태") ShowStatus showStatus,
                        @Parameter(description = "판매 상태") SaleStatus saleStatus,
                        @Parameter(description = "정렬 기준", example = "schedule, salePeriod, id") String sort,
                        @Parameter(description = "페이지 번호") Integer page,
                        @Parameter(description = "페이지 크기") Integer size) {
                public AdminListRequest {
                        if (page == null)
                                page = 0;
                        if (size == null)
                                size = 20;
                }
        }

        @Builder
        @Schema(description = "공연 등록 요청")
        public record CreateRequest(
                        @Schema(description = "공연 제목", example = "레미제라블") @NotBlank String title,
                        @Schema(description = "장르", example = "MUSICAL") @NotNull String genre,
                        @Schema(description = "장소 정보") @Valid @NotNull LocationRequest location,
                        @Schema(description = "상영 시간", example = "150분") @NotBlank String runningTime,
                        @Schema(description = "출연진 정보", example = "홍길동, 김철수") String cast,
                        @Schema(description = "판매 기간") @Valid @NotNull SalePeriodRequest salePeriod,
                        @Schema(description = "스케줄 목록") @Valid @NotEmpty List<ScheduleRequest> schedules) {
                @Builder
                @Schema(description = "장소 정보 요청")
                public record LocationRequest(
                                @Schema(description = "지역") @NotNull Region region,
                                @Schema(description = "공연장명") @NotBlank String venueName,
                                @Schema(description = "홀명") @NotBlank String hallName) {
                }

                @Builder
                @Schema(description = "판매 기간 요청")
                public record SalePeriodRequest(
                                @Schema(description = "시작일시") @NotNull LocalDateTime startDate,
                                @Schema(description = "종료일시") @NotNull LocalDateTime endDate) {
                }

                @Builder
                @Schema(description = "스케줄 등록 요청")
                public record ScheduleRequest(
                                @Schema(description = "공연일", example = "2026-01-15") @NotNull LocalDate showDate,
                                @Schema(description = "공연 시간", example = "19:00") @NotNull String showTime,
                                @Schema(description = "티켓 오픈 시간") @NotNull LocalDateTime ticketOpenTime) {
                }
        }

        @Builder
        @Schema(description = "공연 수정 요청")
        public record UpdateRequest(
                        @Schema(description = "공연 제목") String title,
                        @Schema(description = "장르") String genre,
                        @Schema(description = "장소 정보") @Valid LocationRequest location,
                        @Schema(description = "상영 시간") String runningTime,
                        @Schema(description = "출연진") String cast,
                        @Schema(description = "판매 기간") @Valid SalePeriodRequest salePeriod,
                        @Schema(description = "스케줄 목록 (추가/수정)") @Valid List<ScheduleUpdateRequest> schedules,
                        @Schema(description = "삭제할 스케줄 ID 목록") List<Long> deletedScheduleIds,
                        @Schema(description = "삭제할 이미지 ID 목록") List<Long> deletedDetailImageIds) {
                @Builder
                @Schema(description = "장소 수정 요청")
                public record LocationRequest(
                                @Schema(description = "지역") Region region,
                                @Schema(description = "공연장명") String venueName,
                                @Schema(description = "홀명") String hallName) {
                }

                @Builder
                @Schema(description = "판매 기간 수정 요청")
                public record SalePeriodRequest(
                                @Schema(description = "시작일시") LocalDateTime startDate,
                                @Schema(description = "종료일시") LocalDateTime endDate) {
                }

                @Builder
                @Schema(description = "스케줄 수정 요청")
                public record ScheduleUpdateRequest(
                                @Schema(description = "스케줄 ID (null이면 추가)") Long scheduleId,
                                @Schema(description = "공연일") LocalDate showDate,
                                @Schema(description = "공연 시간") String showTime,
                                @Schema(description = "티켓 오픈 시간") LocalDateTime ticketOpenTime) {
                }
        }

        @Schema(description = "판매 상태 변경 요청")
        public record SaleStatusUpdateRequest(
                        @Schema(description = "판매 상태 (ALLOWED/SUSPENDED)", example = "SUSPENDED") @NotNull @JsonProperty("saleStatus") SaleStatus saleStatus) {
        }

        // ===== Response DTOs =====

        @Builder
        @Schema(description = "공연 목록 조회 응답")
        public record ListResponse(
                        @Schema(description = "공연 ID", example = "1") Long id,
                        @Schema(description = "제목", example = "레미제라블") String title,
                        @Schema(description = "장르", example = "MUSICAL") String genre,
                        @Schema(description = "상태", example = "ON_SALE") String status,
                        @Schema(description = "판매 허용 여부") String saleStatus,
                        @Schema(description = "포스터 URL") String posterUrl,
                        @Schema(description = "장소 정보") LocationInfo location,
                        @Schema(description = "판매 기간") SalePeriod salePeriod,
                        @Schema(description = "생성일") LocalDateTime createdAt,
                        @Schema(description = "조회수") Long viewCount,
                        @Schema(description = "공연 시작일") LocalDate startDate,
                        @Schema(description = "공연 종료일") LocalDate endDate,
                        @Schema(description = "일정 목록") List<ScheduleInfo> schedules) {
                @Builder
                public record LocationInfo(String region, String venue, String hallName) {
                }

                @Builder
                public record SalePeriod(LocalDateTime startDate, LocalDateTime endDate) {
                }

                @Builder
                public record ScheduleInfo(Long keyId, @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
                                @JsonFormat(pattern = "HH:mm") LocalTime time, Integer session) {
                }
        }

        @Builder
        @Schema(description = "관리자용 공연 목록 조회 응답")
        public record AdminListResponse(
                        @Schema(description = "공연 ID", example = "1") Long id,
                        @Schema(description = "제목", example = "레미제라블") String title,
                        @Schema(description = "장르", example = "MUSICAL") String genre,
                        @Schema(description = "상태", example = "ON_SALE") String status,
                        @Schema(description = "판매 허용 여부") String saleStatus,
                        @Schema(description = "포스터 URL") String posterUrl,
                        @Schema(description = "장소 정보") LocationInfo location,
                        @Schema(description = "판매 기간") SalePeriod salePeriod,
                        @Schema(description = "생성일") LocalDateTime createdAt,
                        @Schema(description = "일정 목록") List<ScheduleInfo> schedules) {
                @Builder
                public record LocationInfo(String region, String venue, String hallName) {
                }

                @Builder
                public record SalePeriod(LocalDateTime startDate, LocalDateTime endDate) {
                }

                @Builder
                public record ScheduleInfo(Long keyId, @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
                                @JsonFormat(pattern = "HH:mm") LocalTime time, Integer session) {
                }
        }

        @Builder
        @Schema(description = "공연 상세 조회 응답 (사용자용)")
        public record DetailResponse(
                        @Schema(description = "공연 ID", example = "1") Long id,
                        @Schema(description = "제목") String title,
                        @Schema(description = "장르") String genre,
                        @Schema(description = "상태") String status,
                        @Schema(description = "포스터 URL") String posterUrl,
                        @Schema(description = "상세 이미지 URLs") List<String> detailImageUrls,
                        @Schema(description = "장소 정보") LocationInfo location,
                        @Schema(description = "상영 시간") String runningTime,
                        @Schema(description = "출연진") String cast,
                        @Schema(description = "판매 기간") SalePeriod salePeriod,
                        @Schema(description = "일정 목록") List<ScheduleInfo> schedules,
                        @Schema(description = "현재 서버 시간") LocalDateTime serverCurrentTime) {
                @Builder
                public record LocationInfo(String region, String venue, String hallName) {
                }

                @Builder
                public record SalePeriod(LocalDateTime startDate, LocalDateTime endDate) {
                }

                @Builder
                public record ScheduleInfo(
                                Long keyId,
                                @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
                                @JsonFormat(pattern = "HH:mm") LocalTime time,
                                Integer session) {
                }
        }

        @Builder
        @Schema(description = "공연 상세 조회 응답 (관리자용)")
        public record AdminDetailResponse(
                        @Schema(description = "공연 ID") Long id,
                        @Schema(description = "제목") String title,
                        @Schema(description = "장르") String genre,
                        @Schema(description = "상태") String status,
                        @Schema(description = "판매 상태") String saleStatus,
                        @Schema(description = "포스터 URL") String posterUrl,
                        @Schema(description = "상영 시간") String runningTime,
                        @Schema(description = "출연진") String cast,
                        @Schema(description = "공연장") String venueName,
                        @Schema(description = "홀") String hallName,
                        @Schema(description = "지역") String region,
                        @Schema(description = "판매 시작일") LocalDateTime saleStartDate,
                        @Schema(description = "판매 종료일") LocalDateTime saleEndDate,
                        @Schema(description = "일정 목록") List<AdminScheduleInfo> schedules,
                        @Schema(description = "가격 목록") List<SeatPriceInfo> seatPrices,
                        @Schema(description = "이미지 목록") List<DetailImageInfo> detailImages,
                        @Schema(description = "생성일") LocalDateTime createdAt,
                        @Schema(description = "수정일") LocalDateTime updatedAt) {
                @Builder
                public record AdminScheduleInfo(
                                Long scheduleId,
                                @JsonFormat(pattern = "yyyy-MM-dd") LocalDate showDate,
                                @JsonFormat(pattern = "HH:mm") LocalTime showTime,
                                LocalDateTime ticketOpenTime,
                                Integer remainingSeats,
                                Integer totalSeats,
                                Integer reservationCount) {
                }

                @Builder
                public record SeatPriceInfo(String sectionId, String sectionName, Integer price) {
                }

                @Builder
                public record DetailImageInfo(Long id, String url) {
                }
        }

        @Schema(description = "기본 생성 응답")
        public record CreateResponse(Long showId, String message) {
        }

        @Schema(description = "기본 수정 응답")
        public record UpdateResponse(Long showId, String message) {
        }

        @Schema(description = "기본 삭제 응답")
        public record DeleteResponse(String message) {
        }

        @Schema(description = "판매 상태 변경 응답")
        public record SaleStatusUpdateResponse(Long showId, SaleStatus saleStatus, String message) {
        }

        @Builder
        @Schema(description = "회차 조회 응답")
        public record ScheduleListResponse(
                        @Schema(description = "스케줄 ID", example = "10") Long keyId,
                        @Schema(description = "날짜") LocalDate date,
                        @Schema(description = "시간") @JsonFormat(pattern = "HH:mm") LocalTime time,
                        @Schema(description = "매진 여부") boolean isSoldOut,
                        @Schema(description = "전체 좌석") Integer totalSeats,
                        @Schema(description = "잔여 좌석") Integer remainingSeats,
                        @Schema(description = "좌석 등급별 현황") List<SeatGradeStats> seatGrades) {
                @Builder
                public record SeatGradeStats(
                                @Schema(description = "구역명") String sectionName,
                                @Schema(description = "가격") Integer price,
                                @Schema(description = "잔여석") Integer remainingSeats,
                                @Schema(description = "전체석") Integer totalSeats) {
                }
        }

        @Builder
        @Schema(description = "좌석 현황 응답")
        public record SeatAvailabilityResponse(
                        Long scheduleId,
                        Long showId,
                        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate showDate,
                        @JsonFormat(pattern = "HH:mm") LocalTime showTime,
                        List<SeatAvailability> seatAvailability,
                        Integer totalSeats,
                        Integer totalRemainingSeats,
                        Integer totalAvailabilityRate) {
                @Builder
                public record SeatAvailability(
                                String sectionId,
                                String sectionName,
                                Integer price,
                                Integer totalSeats,
                                Integer remainingSeats,
                                Integer availabilityRate) {
                }
        }
}