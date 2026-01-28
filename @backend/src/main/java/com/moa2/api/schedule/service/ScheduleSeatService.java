package com.moa2.api.schedule.service;

import com.moa2.api.schedule.dto.ScheduleDto;
import com.moa2.api.seatmap.entity.SeatMap;
import com.moa2.api.seatmap.seatmap.repository.SeatMapRepository;
import com.moa2.api.show.domain.entity.*;
import com.moa2.api.show.domain.repository.*;
import com.moa2.global.model.SeatStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

        /**
         * 회차 좌석 상태 조회 (상세 좌석 선택 화면용)
         * - seats + schedule_seats 데이터를 합산하여 응답
         * - schedule_seats가 없으면 자동으로 생성 (lazy initialization)
         */
        @Transactional
        public ScheduleDto.SeatsResponse getScheduleSeats(Long scheduleId) {
                log.debug("좌석 상태 조회 시작: scheduleId={}", scheduleId);

                // 스케줄 존재 여부 확인
                ShowSchedule schedule = showScheduleRepository.findById(scheduleId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스케줄입니다."));

                List<ScheduleSeat> scheduleSeats = scheduleSeatRepository.findSeatMapByScheduleId(scheduleId);
                log.debug("조회된 ScheduleSeat 개수: {}", scheduleSeats.size());

                // ScheduleSeat가 없으면 자동으로 생성
                if (scheduleSeats.isEmpty()) {
                        log.info("ScheduleSeat가 없어 자동 생성 시작: scheduleId={}", scheduleId);
                        scheduleSeats = initializeScheduleSeats(schedule);
                        log.info("ScheduleSeat 자동 생성 완료: scheduleId={}, 생성된 좌석 수={}", scheduleId, scheduleSeats.size());
                }

                List<ScheduleDto.SeatInfo> seats = scheduleSeats.stream()
                                .map(ss -> {
                                        // seatId를 "구역-번호" 형식으로 생성 (예: "A-1")
                                        String sectionName = ss.getGrade().getSection().getName();
                                        String seatIdStr = sectionName + "-" + ss.getSeat().getSeatNumber();

                                        return ScheduleDto.SeatInfo.builder()
                                                        .scheduleSeatId(ss.getId())
                                                        .seatId(seatIdStr)
                                                        .sectionId(ss.getGrade().getSection().getId().toString())
                                                        .row(ss.getSeat().getSeatRow())
                                                        .number(ss.getSeat().getSeatNumber())
                                                        .x(ss.getSeat().getX())
                                                        .y(ss.getSeat().getY())
                                                        .status(ss.getStatus())
                                                        .build();
                                })
                                .collect(Collectors.toList());

                log.debug("최종 반환 좌석 수: {}", seats.size());
                return ScheduleDto.SeatsResponse.builder()
                                .maxSelectable(6) // 한번 예매시 최대 6석까지만 선택 가능
                                .seats(seats)
                                .build();
        }

        /**
         * ScheduleSeat 자동 초기화
         * - Venue의 모든 Seat를 조회
         * - Show의 ShowSeatGrade를 조회
         * - Seat와 ShowSeatGrade를 매칭하여 ScheduleSeat 생성
         */
        private List<ScheduleSeat> initializeScheduleSeats(ShowSchedule schedule) {
                Show show = schedule.getShow();
                if (show.getVenue() == null) {
                        throw new IllegalArgumentException("공연에 연결된 공연장 정보가 없습니다.");
                }

                // Venue의 모든 Seat 조회
                List<Seat> seats = seatRepository.findByVenueId(show.getVenue().getId());
                if (seats.isEmpty()) {
                        log.warn("Venue에 좌석이 없습니다: venueId={}", show.getVenue().getId());
                        return new ArrayList<>();
                }

                // Show의 ShowSeatGrade 조회
                List<ShowSeatGrade> seatGrades = showSeatGradeRepository.findByShowId(show.getId());
                if (seatGrades.isEmpty()) {
                        log.warn("Show에 좌석 등급이 없습니다: showId={}", show.getId());
                        return new ArrayList<>();
                }

                // Section ID로 ShowSeatGrade 매핑
                Map<Long, ShowSeatGrade> gradeMap = seatGrades.stream()
                                .collect(Collectors.toMap(
                                                grade -> grade.getSection().getId(),
                                                grade -> grade,
                                                (existing, replacement) -> existing));

                // ScheduleSeat 생성
                List<ScheduleSeat> scheduleSeatsToSave = new ArrayList<>();
                for (Seat seat : seats) {
                        ShowSeatGrade grade = gradeMap.get(seat.getSection().getId());
                        if (grade == null) {
                                log.warn("Seat의 Section에 해당하는 ShowSeatGrade가 없습니다: seatId={}, sectionId={}",
                                                seat.getId(), seat.getSection().getId());
                                continue;
                        }

                        ScheduleSeat scheduleSeat = ScheduleSeat.builder()
                                        .schedule(schedule)
                                        .seat(seat)
                                        .grade(grade)
                                        .build();
                        scheduleSeatsToSave.add(scheduleSeat);
                }

                // 일괄 저장
                List<ScheduleSeat> saved = scheduleSeatRepository.saveAll(scheduleSeatsToSave);
                log.info("ScheduleSeat 초기화 완료: scheduleId={}, 생성된 좌석 수={}", schedule.getId(), saved.size());

                // 저장된 데이터를 다시 조회 (fetch join 포함)
                return scheduleSeatRepository.findSeatMapByScheduleId(schedule.getId());
        }

        /**
         * 좌석 배치도 조회 (canvas + sections)
         * - seat_maps 테이블에서 canvas와 sections 정보 조회
         */
        @Transactional(readOnly = true)
        public ScheduleDto.SeatMapResponse getSeatMap(Long scheduleId) {
                // 스케줄 존재 여부 확인
                ShowSchedule schedule = showScheduleRepository.findById(scheduleId)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스케줄입니다."));

                // Venue 정보로 SeatMap 조회
                if (schedule.getShow().getVenue() == null) {
                        throw new IllegalArgumentException("공연에 연결된 공연장 정보가 없습니다.");
                }

                SeatMap seatMap = seatMapRepository
                                .findByRegionAndVenueNameAndHallName(
                                                schedule.getShow().getVenue().getRegion(),
                                                schedule.getShow().getVenue().getName(),
                                                schedule.getShow().getVenue().getHallName())
                                .orElseThrow(() -> new IllegalArgumentException("좌석 배치도 정보를 찾을 수 없습니다."));

                // Canvas 정보 변환
                Map<String, Object> canvas = seatMap.getCanvas();
                ScheduleDto.CanvasInfo canvasInfo = ScheduleDto.CanvasInfo.builder()
                                .width((Integer) canvas.get("width"))
                                .height((Integer) canvas.get("height"))
                                .seatRadius((Integer) canvas.get("seatRadius"))
                                .rowGap(canvas.get("rowGap") != null ? (Integer) canvas.get("rowGap") : null)
                                .columnGap(canvas.get("columnGap") != null ? (Integer) canvas.get("columnGap") : null)
                                .build();

                // Sections 정보 변환
                List<Map<String, Object>> sections = seatMap.getSections();
                if (sections == null || sections.isEmpty()) {
                        throw new IllegalArgumentException("좌석 배치도에 구역 정보가 없습니다.");
                }

                List<ScheduleDto.SectionInfo> sectionInfos = sections.stream()
                                .map(section -> ScheduleDto.SectionInfo.builder()
                                                .sectionId((String) section.get("sectionId"))
                                                .name((String) section.get("name"))
                                                .price((Integer) section.get("price"))
                                                .color((String) section.get("color"))
                                                .build())
                                .collect(Collectors.toList());

                return ScheduleDto.SeatMapResponse.builder()
                                .canvas(canvasInfo)
                                .sections(sectionInfos)
                                .build();
        }
}
