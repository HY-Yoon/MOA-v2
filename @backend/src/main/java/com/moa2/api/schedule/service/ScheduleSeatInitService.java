package com.moa2.api.schedule.service;

import com.moa2.api.show.domain.entity.ScheduleSeat;
import com.moa2.api.show.domain.repository.ScheduleSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    private long toMs(long nanos) {
        return nanos / 1_000_000;
    }
}
