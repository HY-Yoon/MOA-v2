package com.moa2.api.schedule.service;

import com.moa2.api.schedule.dto.ScheduleSeatsResponse;
import com.moa2.domain.show.entity.ScheduleSeat;
import com.moa2.domain.show.repository.ScheduleSeatRepository;
import com.moa2.domain.show.repository.ShowScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 회차 좌석 배치도 조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleSeatService {

    private final ShowScheduleRepository showScheduleRepository;
    private final ScheduleSeatRepository scheduleSeatRepository;

    /**
     * 회차 좌석 배치도 조회
     */
    public ScheduleSeatsResponse getScheduleSeats(Long scheduleId) {
        // 스케줄 존재 여부 확인
        showScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스케줄입니다."));

        List<ScheduleSeat> scheduleSeats = scheduleSeatRepository.findSeatMapByScheduleId(scheduleId);

        List<ScheduleSeatsResponse.SeatInfo> seats = scheduleSeats.stream()
                .map(ss -> ScheduleSeatsResponse.SeatInfo.builder()
                        .seatId(ss.getSeat().getId())
                        .row(ss.getSeat().getSeatRow())
                        .col(ss.getSeat().getSeatNumber())
                        .grade(ss.getGrade().getSection().getName())
                        .price(ss.getGrade().getPrice())
                        .status(ss.getStatus())
                        .build())
                .collect(Collectors.toList());

        return ScheduleSeatsResponse.builder()
                .scheduleId(scheduleId)
                .seats(seats)
                .build();
    }
}

