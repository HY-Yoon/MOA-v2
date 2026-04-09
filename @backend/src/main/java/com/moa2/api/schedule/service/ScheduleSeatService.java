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

        private static final int MAX_SELECTABLE_SEATS = 6;

        /**
         * 회차 좌석 상태 조회 (상세 좌석 선택 화면용)
         * - 조회 전용: 데이터 생성/보정은 등록 시점에서 선처리
         */
        @Transactional
        public ScheduleDto.SeatsResponse getScheduleSeats(Long scheduleId) {
                long startNs = System.nanoTime();
                log.debug("좌석 상태 조회 시작: scheduleId={}", scheduleId);

                showScheduleRepository.findById(scheduleId)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스케줄입니다."));
                long afterScheduleLookupNs = System.nanoTime();

                List<ScheduleSeat> scheduleSeats = scheduleSeatRepository.findSeatMapByScheduleId(scheduleId);
                long afterInitialSeatQueryNs = System.nanoTime();
                int initialSeatCount = scheduleSeats.size();

                if (scheduleSeats.isEmpty()) {
                        log.error("회차 좌석 데이터 누락: scheduleId={}", scheduleId);
                        throw new IllegalStateException("회차 좌석 데이터가 준비되지 않았습니다. 관리자에게 문의하세요.");
                }
                long afterValidationNs = System.nanoTime();
                int finalSeatCount = scheduleSeats.size();

                // DTO 변환
                List<ScheduleDto.SeatInfo> seats = scheduleSeats.stream()
                        .map(this::mapToSeatInfo)
                        .collect(Collectors.toList());
                long afterDtoMappingNs = System.nanoTime();

                log.info(
                        "좌석 조회 타이밍: scheduleId={}, scheduleLookupMs={}, initialSeatQueryMs={}, validationMs={}, dtoMapMs={}, totalMs={}, initialSeatCount={}, finalSeatCount={}",
                        scheduleId,
                        toMs(afterScheduleLookupNs - startNs),
                        toMs(afterInitialSeatQueryNs - afterScheduleLookupNs),
                        toMs(afterValidationNs - afterInitialSeatQueryNs),
                        toMs(afterDtoMappingNs - afterValidationNs),
                        toMs(afterDtoMappingNs - startNs),
                        initialSeatCount,
                        finalSeatCount
                );

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

        private long toMs(long nanos) {
                return nanos / 1_000_000;
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