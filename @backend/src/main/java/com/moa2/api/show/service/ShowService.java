package com.moa2.api.show.service;

import com.moa2.api.reservation.domain.repository.ReservationRepository;
import com.moa2.api.show.domain.entity.*;
import com.moa2.api.show.domain.repository.*;
import com.moa2.api.show.dto.ShowDto;
import com.moa2.global.model.SeatStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 사용자용 공연 조회 서비스
 */
@Slf4j
@Profile("v2")
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShowService {

        private final ShowRepository showRepository;
        private final ShowScheduleRepository showScheduleRepository;
        private final ShowSeatGradeRepository showSeatGradeRepository;
        private final SeatRepository seatRepository;
        private final ReservationRepository reservationRepository;
        private final ScheduleSeatRepository scheduleSeatRepository;

        /**
         * 공연 목록 조회 (사용자용)
         * - 판매 허용(ALLOWED)된 공연만
         * - 판매중(ON_SALE) 또는 매진(SOLD_OUT) 상태만
         */
        public Page<ShowDto.ListResponse> getShowList(ShowDto.ListRequest request, Pageable pageable) {
                String keywordPattern = (request.keyword() != null && !request.keyword().trim().isEmpty())
                                ? "%" + request.keyword().trim().toLowerCase() + "%"
                                : null;

                Page<Show> shows = showRepository.findShowsForUser(
                                request.genre(),
                                request.region(),
                                keywordPattern,
                                request.startDate(),
                                request.endDate(),
                                pageable);

                // N+1 방지: 조회된 공연들의 모든 스케줄을 한 번에 조회
                List<Long> showIds = shows.getContent().stream()
                                .map(Show::getId)
                                .collect(Collectors.toList());

                List<ShowSchedule> allSchedules = showScheduleRepository.findAllByShowIdInOrderByDateAndTime(showIds);

                // 스케줄을 공연 ID별로 그룹화
                Map<Long, List<ShowSchedule>> schedulesByShowId = allSchedules.stream()
                                .collect(Collectors.groupingBy(s -> s.getShow().getId()));

                List<ShowDto.ListResponse> content = shows.getContent().stream()
                                .map(show -> ShowDto.from(show,
                                                schedulesByShowId.getOrDefault(show.getId(), List.of())))
                                .collect(Collectors.toList());

                return new PageImpl<>(content, pageable, shows.getTotalElements());
        }

        /**
         * 공연 상세 조회 (사용자용)
         * 조회 시 viewCount 자동 증가
         */
        @Transactional
        public ShowDto.DetailResponse getShowDetail(Long id) {
                Show show = showRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("공연을 찾을 수 없습니다"));

                // viewCount 증가
                increaseViewCount(show);

                // 일정 정보 조회 & 변환 (회차 계산 필요)
                List<ShowSchedule> schedules = showScheduleRepository.findByShowIdOrderByDateAndTime(id);

                return ShowDto.of(show, schedules);
        }

        /**
         * 날짜별 회차 조회
         */
        @Cacheable(value = "scheduleSeats", key = "#showId + '_' + #date", cacheManager = "shortTtlCacheManager", sync = true)
        public List<ShowDto.ScheduleListResponse> getShowSchedules(Long showId, LocalDate date) {
                // 공연 존재 여부 확인
                if (!showRepository.existsById(showId)) {
                        throw new RuntimeException("공연을 찾을 수 없습니다");
                }

                List<ShowSchedule> schedules = showScheduleRepository.findByShowIdAndOptionalDate(showId, date);
                if (schedules.isEmpty()) {
                        return List.of();
                }

                List<Long> scheduleIds = schedules.stream().map(ShowSchedule::getId).collect(Collectors.toList());

                // 1. 전체/잔여 좌석 집계
                List<Object[]> totalSeatStats = scheduleSeatRepository
                                .countTotalAndRemainingSeatsByScheduleIds(scheduleIds);
                Map<Long, int[]> seatCountsMap = totalSeatStats.stream()
                                .collect(Collectors.toMap(
                                                row -> (Long) row[0],
                                                row -> new int[] { ((Number) row[1]).intValue(),
                                                                ((Number) row[2]).intValue() })); // key: scheduleId,
                                                                                                  // value: [total,
                                                                                                  // remaining]

                // 2. 등급별 통계 집계
                List<Object[]> gradeStats = scheduleSeatRepository.countSeatGradeStatsByScheduleIds(scheduleIds);
                Map<Long, List<ShowDto.ScheduleListResponse.SeatGradeStats>> gradeStatsMap = gradeStats.stream()
                                .collect(Collectors.groupingBy(
                                                row -> (Long) row[0],
                                                Collectors.mapping(row -> ShowDto.ScheduleListResponse.SeatGradeStats
                                                                .builder()
                                                                .sectionName((String) row[2])
                                                                .price((Integer) row[3])
                                                                .remainingSeats(((Number) row[4]).intValue())
                                                                .totalSeats(((Number) row[5]).intValue())
                                                                .build(), Collectors.toList())));

                return schedules.stream()
                                .map(schedule -> {
                                        int[] seatCounts = seatCountsMap.getOrDefault(schedule.getId(),
                                                        new int[] { 0, 0 });
                                        int totalSeats = seatCounts[0];
                                        int availableSeats = seatCounts[1];
                                        boolean isSoldOut = totalSeats > 0 && availableSeats <= 0;

                                        List<ShowDto.ScheduleListResponse.SeatGradeStats> seatGradeStats = gradeStatsMap
                                                        .getOrDefault(schedule.getId(), List.of());

                                        return new ShowDto.ScheduleListResponse(
                                                        schedule.getId(),
                                                        schedule.getShowDate(),
                                                        schedule.getShowTime(),
                                                        isSoldOut,
                                                        totalSeats,
                                                        availableSeats,
                                                        seatGradeStats);
                                })
                                .collect(Collectors.toList());
        }

        // --- Private Helper Methods (Mapping Logic) ---

        private void increaseViewCount(Show show) {
                show.increaseViewCount();
                showRepository.save(show);
        }
}