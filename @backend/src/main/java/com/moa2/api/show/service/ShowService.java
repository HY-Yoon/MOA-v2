package com.moa2.api.show.service;

import com.moa2.api.show.dto.*;
import com.moa2.domain.show.entity.*;
import com.moa2.domain.show.repository.*;
import com.moa2.domain.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 사용자용 공연 조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShowService {

    private final ShowRepository showRepository;
    private final ShowScheduleRepository showScheduleRepository;
    private final ShowSeatGradeRepository showSeatGradeRepository;
    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;

    /**
     * 공연 목록 조회 (사용자용)
     * - 판매 허용(ALLOWED)된 공연만
     * - 판매중(ON_SALE) 또는 매진(SOLD_OUT) 상태만
     */
    @Transactional(readOnly = true)
    public Page<ShowListResponse> getShowList(ShowListRequest request, Pageable pageable) {
        log.debug("사용자 공연 목록 조회 요청: genre={}, region={}, keyword={}, startDate={}, endDate={}", 
            request.getGenre(), request.getRegion(), request.getKeyword(), 
            request.getStartDate(), request.getEndDate());
        
        // keyword가 있으면 LIKE 검색을 위해 % 추가
        String keywordPattern = null;
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            keywordPattern = "%" + request.getKeyword().trim() + "%";
        }
        
        log.debug("Repository 호출 전 파라미터: genre={}, region={}, keywordPattern={}, startDate={}, endDate={}", 
            request.getGenre(), request.getRegion(), keywordPattern, 
            request.getStartDate(), request.getEndDate());
        
        Page<Show> shows = showRepository.findShowsForUser(
            request.getGenre(),
            request.getRegion(),
            keywordPattern,
            request.getStartDate(),
            request.getEndDate(),
            pageable
        );

        List<ShowListResponse> content = shows.getContent().stream()
            .map(show -> {
                // 모든 일정 조회
                List<ShowSchedule> allSchedules = showScheduleRepository
                    .findByShowIdOrderByDateAndTime(show.getId());

                // 일정 목록 생성 (회차 자동 계산)
                Map<java.time.LocalDate, Integer> sessionCountByDate = new HashMap<>();
                List<ShowListResponse.ScheduleInfo> scheduleInfos = allSchedules.stream()
                    .map(schedule -> {
                        // 같은 날짜의 회차 계산
                        java.time.LocalDate date = schedule.getShowDate();
                        int session = sessionCountByDate.getOrDefault(date, 0) + 1;
                        sessionCountByDate.put(date, session);

                        return ShowListResponse.ScheduleInfo.builder()
                            .keyId(schedule.getId())
                            .date(schedule.getShowDate())
                            .time(schedule.getShowTime())
                            .session(session)
                            .build();
                    })
                    .collect(Collectors.toList());

                // 판매 기간 생성
                ShowListResponse.SalePeriod salePeriod = null;
                if (show.getSaleStartDate() != null || show.getSaleEndDate() != null) {
                    salePeriod = ShowListResponse.SalePeriod.builder()
                        .startDate(show.getSaleStartDate())
                        .endDate(show.getSaleEndDate())
                        .build();
                }

                // 장소 정보 생성
                ShowListResponse.LocationInfo location = null;
                if (show.getVenue() != null) {
                    location = ShowListResponse.LocationInfo.builder()
                        .region(show.getVenue().getRegion() != null ? show.getVenue().getRegion().name() : null)
                        .venue(show.getVenue().getName())
                        .hallName(show.getVenue().getHallName())
                        .build();
                }

                return ShowListResponse.builder()
                    .id(show.getId())
                    .title(show.getTitle())
                    .genre(show.getGenre() != null ? show.getGenre().name() : null)
                    .status(show.getStatus() != null ? show.getStatus().name() : null)
                    .posterUrl(show.getPosterUrl())
                    .location(location)
                    .salePeriod(salePeriod)
                    .schedules(scheduleInfos)
                    .build();
            })
            .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, shows.getTotalElements());
    }

    /**
     * 공연 상세 조회 (사용자용)
     * ⭐ 조회 시 viewCount 자동 증가
     */
    @Transactional
    public ShowDetailResponse getShowDetail(Long id) {
        log.debug("사용자 공연 상세 조회 요청: showId={}", id);
        
        Show show = showRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("공연을 찾을 수 없습니다"));

        // ⭐ viewCount 증가 (사용자만)
        if (show.getViewCount() == null) {
            show.setViewCount(0L);
        }
        show.setViewCount(show.getViewCount() + 1);
        showRepository.save(show);
        log.debug("공연 조회수 증가: showId={}, newViewCount={}", id, show.getViewCount());

        // 일정 정보 조회
        List<ShowSchedule> schedules = showScheduleRepository.findByShowIdOrderByDateAndTime(id);
        List<ShowDetailResponse.ScheduleInfo> scheduleInfos = schedules.stream()
            .map(schedule -> {
                Long totalSeats = show.getVenue() != null 
                    ? seatRepository.countByVenueId(show.getVenue().getId()) 
                    : 0L;
                Long reservationCount = reservationRepository.countByScheduleId(schedule.getId());
                Long remainingSeats = totalSeats - reservationCount;

                return ShowDetailResponse.ScheduleInfo.builder()
                    .scheduleId(schedule.getId())
                    .showDate(schedule.getShowDate())
                    .showTime(schedule.getShowTime())
                    .ticketOpenTime(schedule.getTicketOpenTime())
                    .remainingSeats(remainingSeats.intValue())
                    .totalSeats(totalSeats.intValue())
                    .build();
            })
            .collect(Collectors.toList());

        // 좌석 가격 정보 조회
        List<ShowSeatGrade> seatGrades = showSeatGradeRepository.findByShowId(id);
        List<ShowDetailResponse.SeatGradeInfo> seatGradeInfos = seatGrades.stream()
            .map(grade -> ShowDetailResponse.SeatGradeInfo.builder()
                .sectionId(grade.getSection().getId().toString())
                .sectionName(grade.getSection().getName())
                .price(grade.getPrice())
                .build())
            .collect(Collectors.toList());

        // 판매 기간 생성
        ShowDetailResponse.SalePeriod salePeriod = null;
        if (show.getSaleStartDate() != null || show.getSaleEndDate() != null) {
            salePeriod = ShowDetailResponse.SalePeriod.builder()
                .startDate(show.getSaleStartDate())
                .endDate(show.getSaleEndDate())
                .build();
        }

        // 장소 정보 생성
        ShowDetailResponse.LocationInfo location = null;
        if (show.getVenue() != null) {
            location = ShowDetailResponse.LocationInfo.builder()
                .region(show.getVenue().getRegion() != null ? show.getVenue().getRegion().name() : null)
                .venue(show.getVenue().getName())
                .hallName(show.getVenue().getHallName())
                .build();
        }

        return ShowDetailResponse.builder()
            .id(show.getId())
            .title(show.getTitle())
            .genre(show.getGenre() != null ? show.getGenre().name() : null)
            .status(show.getStatus() != null ? show.getStatus().name() : null)
            .posterUrl(show.getPosterUrl())
            .detailImageUrls(show.getDetailImageUrls())
            .location(location)
            .runningTime(show.getRunningTime())
            .cast(show.getCast())
            .salePeriod(salePeriod)
            .schedules(scheduleInfos)
            .seatGrades(seatGradeInfos)
            .build();
    }

    /**
     * 스케줄별 잔여석 조회
     */
    @Transactional(readOnly = true)
    public ScheduleSeatAvailabilityResponse getScheduleSeatAvailability(Long showId, Long scheduleId) {
        log.debug("스케줄별 잔여석 조회 요청: showId={}, scheduleId={}", showId, scheduleId);
        
        Show show = showRepository.findById(showId)
            .orElseThrow(() -> new RuntimeException("공연을 찾을 수 없습니다"));

        ShowSchedule schedule = showScheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new RuntimeException("스케줄을 찾을 수 없습니다"));

        // 스케줄이 해당 공연에 속하는지 확인
        if (!schedule.getShow().getId().equals(showId)) {
            throw new RuntimeException("스케줄이 해당 공연에 속하지 않습니다");
        }

        // 좌석 가격 정보 조회
        List<ShowSeatGrade> seatGrades = showSeatGradeRepository.findByShowId(showId);
        
        // 구역별 잔여석 계산
        List<ScheduleSeatAvailabilityResponse.SeatAvailability> seatAvailabilityList = seatGrades.stream()
            .map(grade -> {
                VenueSeatSection section = grade.getSection();
                
                // 해당 구역의 전체 좌석 수 (venue의 물리적 좌석)
                Long totalSeats = seatRepository.countByVenueId(show.getVenue().getId());
                // TODO: section별로 카운트하는 메서드 필요 (현재는 전체 venue 좌석 수)
                
                // 해당 구역, 스케줄의 예약된 좌석 수
                Long reservedSeats = reservationRepository.countByScheduleId(scheduleId);
                // TODO: section별로 카운트하는 메서드 필요
                
                // 잔여석 계산
                Long remainingSeats = totalSeats - reservedSeats;
                int availabilityRate = totalSeats > 0 ? (int) ((remainingSeats * 100) / totalSeats) : 0;

                return ScheduleSeatAvailabilityResponse.SeatAvailability.builder()
                    .sectionId(section.getId().toString())
                    .sectionName(section.getName())
                    .price(grade.getPrice())
                    .totalSeats(totalSeats.intValue())
                    .remainingSeats(remainingSeats.intValue())
                    .availabilityRate(availabilityRate)
                    .build();
            })
            .collect(Collectors.toList());

        // 전체 통계 계산
        int totalSeats = seatAvailabilityList.stream()
            .mapToInt(ScheduleSeatAvailabilityResponse.SeatAvailability::getTotalSeats)
            .sum();
        int totalRemainingSeats = seatAvailabilityList.stream()
            .mapToInt(ScheduleSeatAvailabilityResponse.SeatAvailability::getRemainingSeats)
            .sum();
        int totalAvailabilityRate = totalSeats > 0 ? (totalRemainingSeats * 100) / totalSeats : 0;

        return ScheduleSeatAvailabilityResponse.builder()
            .scheduleId(schedule.getId())
            .showId(show.getId())
            .showDate(schedule.getShowDate())
            .showTime(schedule.getShowTime())
            .seatAvailability(seatAvailabilityList)
            .totalSeats(totalSeats)
            .totalRemainingSeats(totalRemainingSeats)
            .totalAvailabilityRate(totalAvailabilityRate)
            .build();
    }
}
