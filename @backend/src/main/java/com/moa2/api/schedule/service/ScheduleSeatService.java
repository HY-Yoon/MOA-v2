package com.moa2.api.schedule.service;

import com.moa2.api.schedule.dto.ScheduleDto;
import com.moa2.api.seatmap.domain.entity.SeatMap;
import com.moa2.api.seatmap.domain.repository.SeatMapRepository;
import com.moa2.api.show.domain.entity.*;
import com.moa2.api.show.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 회차 좌석 배치도 조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleSeatService {

        private final ShowScheduleRepository showScheduleRepository;
        private final ScheduleSeatRepository scheduleSeatRepository;
        private final SeatMapRepository seatMapRepository;
        private final SeatRepository seatRepository;
        private final ShowSeatGradeRepository showSeatGradeRepository;
        private final ScheduleSeatInitService scheduleSeatInitService;

        private static final int MAX_SELECTABLE_SEATS = 6;

        /**
         * 회차 좌석 상태 조회 (상세 좌석 선택 화면용)
         * - Lazy Initialization 적용: 데이터가 없으면 자동 생성
         */
        @Transactional
        public ScheduleDto.SeatsResponse getScheduleSeats(Long scheduleId) {
                log.debug("좌석 상태 조회 시작: scheduleId={}", scheduleId);

                ShowSchedule schedule = showScheduleRepository.findById(scheduleId)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스케줄입니다."));

                List<ScheduleSeat> scheduleSeats = scheduleSeatRepository.findSeatMapByScheduleId(scheduleId);

                // 일부/전체 데이터 유실, 동시 초기화 경쟁 상황을 모두 안전하게 보정
                scheduleSeats = ensureScheduleSeats(schedule, scheduleSeats);

                // DTO 변환
                List<ScheduleDto.SeatInfo> seats = scheduleSeats.stream()
                        .map(this::mapToSeatInfo)
                        .collect(Collectors.toList());

                return ScheduleDto.SeatsResponse.builder()
                        .maxSelectable(MAX_SELECTABLE_SEATS)
                        .seats(seats)
                        .build();
        }

        /**
         * 좌석 배치도 조회 (canvas + sections)
         */
        @Transactional(readOnly = true)
        public ScheduleDto.SeatMapResponse getSeatMap(Long scheduleId) {
                ShowSchedule schedule = showScheduleRepository.findById(scheduleId)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스케줄입니다."));

                if (schedule.getShow().getVenue() == null) {
                        throw new IllegalArgumentException("공연에 연결된 공연장 정보가 없습니다.");
                }

                Venue venue = schedule.getShow().getVenue();
                SeatMap seatMap = seatMapRepository
                        .findByRegionAndVenueNameAndHallName(
                                venue.getRegion(),
                                venue.getName(),
                                venue.getHallName())
                        .orElseThrow(() -> new IllegalArgumentException("좌석 배치도 정보를 찾을 수 없습니다."));

                return ScheduleDto.SeatMapResponse.builder()
                        .canvas(mapToCanvasInfo(seatMap.getCanvas()))
                        .sections(mapToSectionInfos(seatMap.getSections()))
                        .build();
        }

        // --- Private Methods (Logic & Mapping) ---

        private List<ScheduleSeat> ensureScheduleSeats(ShowSchedule schedule, List<ScheduleSeat> existingScheduleSeats) {
                Show show = schedule.getShow();
                if (show.getVenue() == null) {
                        throw new IllegalArgumentException("공연에 연결된 공연장 정보가 없습니다.");
                }

                List<Seat> seats = seatRepository.findByVenueId(show.getVenue().getId());
                List<ShowSeatGrade> seatGrades = showSeatGradeRepository.findByShowId(show.getId());

                if (seats.isEmpty() || seatGrades.isEmpty()) {
                        log.warn("초기화 실패: 좌석({}) 또는 등급({}) 정보 부족 - showId={}", seats.size(), seatGrades.size(), show.getId());
                        return existingScheduleSeats;
                }

                Map<Long, ShowSeatGrade> gradeMap = seatGrades.stream()
                        .collect(Collectors.toMap(
                                g -> g.getSection().getId(),
                                g -> g,
                                (existing, replacement) -> existing
                        ));

                Set<Long> existingSeatIds = new HashSet<>();
                for (ScheduleSeat existingSeat : existingScheduleSeats) {
                        existingSeatIds.add(existingSeat.getSeat().getId());
                }

                List<ScheduleSeat> scheduleSeatsToSave = new ArrayList<>();
                for (Seat seat : seats) {
                        if (existingSeatIds.contains(seat.getId())) {
                                continue;
                        }
                        ShowSeatGrade grade = gradeMap.get(seat.getSection().getId());
                        if (grade != null) {
                                scheduleSeatsToSave.add(ScheduleSeat.builder()
                                        .schedule(schedule)
                                        .seat(seat)
                                        .grade(grade)
                                        .build());
                        }
                }

                // 넣을 것이 없으면 기존 목록 그대로 반환 (DB 접근 없음)
                if (scheduleSeatsToSave.isEmpty()) {
                        return existingScheduleSeats;
                }

                // REQUIRES_NEW 트랜잭션으로 INSERT 시도
                // - 성공: 독립 커밋 → 아래 재조회로 최신 상태 반환
                // - 중복 충돌(동시 요청): 독립 롤백 → 외부 트랜잭션은 살아있으므로 아래 재조회 정상 동작
                scheduleSeatInitService.insertMissingSeats(scheduleSeatsToSave, schedule.getId());

                // 영속성 컨텍스트 갱신을 위해 항상 재조회
                return scheduleSeatRepository.findSeatMapByScheduleId(schedule.getId());
        }

        private ScheduleDto.SeatInfo mapToSeatInfo(ScheduleSeat ss) {
                String sectionName = ss.getGrade().getSection().getName().replace("구역", "");
                String seatIdStr = sectionName + "-" + ss.getSeat().getSeatNumber();

                // DTO의 row 필드가 String 타입이므로 String.valueOf 처리
                String rowStr = String.valueOf(ss.getSeat().getSeatRow());

                return ScheduleDto.SeatInfo.builder()
                        .scheduleSeatId(ss.getId())
                        .seatId(seatIdStr)
                        .sectionId(sectionName)
                        .row(rowStr)
                        .number(ss.getSeat().getSeatNumber())
                        .x(ss.getSeat().getX())
                        .y(ss.getSeat().getY())
                        .status(ss.getStatus())
                        .build();
        }

        private ScheduleDto.CanvasInfo mapToCanvasInfo(Map<String, Object> canvasMap) {
                if (canvasMap == null) return null;
                return ScheduleDto.CanvasInfo.builder()
                        .width((Integer) canvasMap.get("width"))
                        .height((Integer) canvasMap.get("height"))
                        .seatRadius((Integer) canvasMap.get("seatRadius"))
                        .rowGap((Integer) canvasMap.getOrDefault("rowGap", null))
                        .columnGap((Integer) canvasMap.getOrDefault("columnGap", null))
                        .build();
        }

        private List<ScheduleDto.SectionInfo> mapToSectionInfos(List<Map<String, Object>> sectionsMap) {
                if (sectionsMap == null) return new ArrayList<>();
                return sectionsMap.stream()
                        .map(section -> ScheduleDto.SectionInfo.builder()
                                .sectionId((String) section.get("sectionId"))
                                .name((String) section.get("name"))
                                .price((Integer) section.get("price"))
                                .color((String) section.get("color"))
                                .build())
                        .collect(Collectors.toList());
        }
}