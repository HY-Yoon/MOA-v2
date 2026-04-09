package com.moa2.api.schedule.service;

import com.moa2.api.show.domain.entity.ScheduleSeat;
import com.moa2.api.show.domain.entity.Seat;
import com.moa2.api.show.domain.entity.Show;
import com.moa2.api.show.domain.entity.ShowSchedule;
import com.moa2.api.show.domain.entity.ShowSeatGrade;
import com.moa2.api.show.domain.repository.SeatRepository;
import com.moa2.api.show.domain.repository.ScheduleSeatRepository;
import com.moa2.api.show.domain.repository.ShowSeatGradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ScheduleSeat 초기 INSERT 전용 서비스
 *
 * 별도 빈으로 분리한 이유:
 * - REQUIRES_NEW 트랜잭션이 Spring AOP 프록시를 통해야 동작함 (self-invocation 불가)
 * - 외부 트랜잭션(getScheduleSeats)과 독립적으로 commit/rollback 되어야
 *   PostgreSQL "transaction aborted" 전파 없이 재조회가 가능함
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleSeatInitService {

    private final ScheduleSeatRepository scheduleSeatRepository;
    private final SeatRepository seatRepository;
    private final ShowSeatGradeRepository showSeatGradeRepository;

    /**
     * 누락된 ScheduleSeat만 INSERT (멱등, REQUIRES_NEW)
     *
     * - 성공 시 독립 커밋 → 외부 트랜잭션에서 재조회 가능
     * - 중복 키 충돌 시 독립 롤백 → 외부 트랜잭션은 유지되므로 재조회 가능
     *
     * @return true: insert 시도함, false: 넣을 것 없음
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean insertMissingSeats(List<ScheduleSeat> scheduleSeatsToSave, Long scheduleId) {
        long startNs = System.nanoTime();
        if (scheduleSeatsToSave.isEmpty()) {
            return false;
        }
        try {
            scheduleSeatRepository.saveAll(scheduleSeatsToSave);
            // unique constraint 충돌을 commit 시점이 아닌 현재 try 블록에서 감지하도록 강제 flush
            scheduleSeatRepository.flush();
            log.info(
                    "ScheduleSeat 보정 완료: scheduleId={}, inserted={}, saveAllMs={}",
                    scheduleId,
                    scheduleSeatsToSave.size(),
                    toMs(System.nanoTime() - startNs)
            );
        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 다른 트랜잭션이 먼저 INSERT한 경우
            // 이 REQUIRES_NEW 트랜잭션만 롤백 → 외부 트랜잭션은 정상 유지
            log.warn(
                    "ScheduleSeat 중복 감지(동시 요청) - 재조회로 처리: scheduleId={}, saveAllMs={}",
                    scheduleId,
                    toMs(System.nanoTime() - startNs)
            );
        }
        return true;
    }

    /**
     * 특정 회차의 schedule_seats를 전체 재생성한다.
     * - 조회 API에서 lazy 생성하지 않고, 등록/수정 시점에 선생성하기 위한 메서드
     */
    @Transactional
    public int rebuildScheduleSeats(ShowSchedule schedule) {
        Show show = schedule.getShow();
        if (show.getVenue() == null) {
            throw new IllegalArgumentException("공연에 연결된 공연장 정보가 없습니다.");
        }

        List<Seat> seats = seatRepository.findByVenueId(show.getVenue().getId());
        List<ShowSeatGrade> seatGrades = showSeatGradeRepository.findByShowId(show.getId());
        if (seats.isEmpty() || seatGrades.isEmpty()) {
            throw new IllegalStateException("회차 좌석 초기화 실패: 좌석 또는 등급 정보가 비어있습니다.");
        }

        Map<Long, ShowSeatGrade> gradeMap = seatGrades.stream()
                .collect(Collectors.toMap(g -> g.getSection().getId(), g -> g, (existing, replacement) -> existing));

        List<ScheduleSeat> toInsert = new ArrayList<>();
        for (Seat seat : seats) {
            ShowSeatGrade grade = gradeMap.get(seat.getSection().getId());
            if (grade == null) {
                continue;
            }
            toInsert.add(ScheduleSeat.builder()
                    .schedule(schedule)
                    .seat(seat)
                    .grade(grade)
                    .build());
        }

        scheduleSeatRepository.deleteByScheduleId(schedule.getId());
        scheduleSeatRepository.saveAll(toInsert);
        scheduleSeatRepository.flush();

        log.info("ScheduleSeat 선생성 완료: scheduleId={}, inserted={}", schedule.getId(), toInsert.size());
        return toInsert.size();
    }

    private long toMs(long nanos) {
        return nanos / 1_000_000;
    }
}
