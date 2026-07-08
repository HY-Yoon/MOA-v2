package com.moa2.api.queue.service;

import com.moa2.api.queue.dto.QueueDto;
import com.moa2.api.queue.queue.entity.Queue;
import com.moa2.api.queue.queue.repository.QueueRepository;
import com.moa2.api.show.domain.entity.ShowSchedule;
import com.moa2.api.show.domain.repository.ShowScheduleRepository;
import com.moa2.api.user.domain.entity.User;
import com.moa2.api.user.domain.repository.UserRepository;
import com.moa2.global.model.QueueStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 대기열 진입·재진입 시 순번 계산 오류로 사용자가 무한 대기하거나 중복 대기열이 생기는 사고를 막는다.
 */
@ExtendWith(MockitoExtension.class)
class QueueServiceTest {

    @Mock
    private QueueRepository queueRepository;

    @Mock
    private ShowScheduleRepository showScheduleRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("앞에 대기자가 없으면 신규 대기열을 즉시 READY로 활성화한다")
    void 앞에_대기자가_없으면_신규_대기열을_즉시_READY로_활성화한다() {
        // given
        QueueService queueService = queueService();
        User user = mock(User.class);
        ShowSchedule schedule = mock(ShowSchedule.class);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(showScheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(queueRepository.findTopByUserIdAndScheduleIdAndStatusInOrderByCreatedAtDesc(
                1L,
                10L,
                List.of(QueueStatus.WAITING, QueueStatus.READY)
        )).thenReturn(Optional.empty());
        when(queueRepository.save(any(Queue.class))).thenAnswer(invocation -> {
            Queue queue = invocation.getArgument(0);
            if (queue.getId() == null) {
                ReflectionTestUtils.setField(queue, "id", 100L);
            }
            return queue;
        });
        when(queueRepository.countWaitingBefore(10L, 100L)).thenReturn(0L);

        // when
        QueueDto.EnterResponse response = queueService.enterQueue(1L, new QueueDto.EnterRequest(10L));

        // then
        ArgumentCaptor<Queue> queueCaptor = ArgumentCaptor.forClass(Queue.class);
        verify(queueRepository).countWaitingBefore(10L, 100L);
        verify(queueRepository, org.mockito.Mockito.times(2)).save(queueCaptor.capture());

        Queue activatedQueue = queueCaptor.getAllValues().get(1);
        assertThat(activatedQueue.getStatus()).isEqualTo(QueueStatus.READY);
        assertThat(activatedQueue.getActiveUntil()).isNotNull();
        assertThat(response.queueId()).isEqualTo(100L);
        assertThat(response.position()).isZero();
    }

    @Test
    @DisplayName("이미 WAITING 상태로 등록된 사용자가 재진입하면 기존 대기열 정보를 반환한다")
    void 이미_WAITING_상태로_등록된_사용자가_재진입하면_기존_대기열_정보를_반환한다() {
        // given
        QueueService queueService = queueService();
        Queue existingQueue = queue(200L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(mock(User.class)));
        when(showScheduleRepository.findById(10L)).thenReturn(Optional.of(mock(ShowSchedule.class)));
        when(queueRepository.findTopByUserIdAndScheduleIdAndStatusInOrderByCreatedAtDesc(
                1L,
                10L,
                List.of(QueueStatus.WAITING, QueueStatus.READY)
        )).thenReturn(Optional.of(existingQueue));
        when(queueRepository.countWaitingBefore(10L, 200L)).thenReturn(3L);

        // when
        QueueDto.EnterResponse response = queueService.enterQueue(1L, new QueueDto.EnterRequest(10L));

        // then
        verify(queueRepository, never()).save(any(Queue.class));
        assertThat(existingQueue.getStatus()).isEqualTo(QueueStatus.WAITING);
        assertThat(response.queueId()).isEqualTo(200L);
        assertThat(response.position()).isEqualTo(3L);
    }

    @Test
    @DisplayName("이미 READY 상태로 등록된 사용자가 재진입하면 기존 정보를 반환하고 새로 등록하지 않는다")
    void 이미_READY_상태로_등록된_사용자가_재진입하면_기존_정보를_반환하고_새로_등록하지_않는다() {
        // given
        QueueService queueService = queueService();
        Queue existingQueue = queue(300L);
        existingQueue.activate(java.time.LocalDateTime.now().plusMinutes(5));

        when(userRepository.findById(1L)).thenReturn(Optional.of(mock(User.class)));
        when(showScheduleRepository.findById(10L)).thenReturn(Optional.of(mock(ShowSchedule.class)));
        when(queueRepository.findTopByUserIdAndScheduleIdAndStatusInOrderByCreatedAtDesc(
                1L,
                10L,
                List.of(QueueStatus.WAITING, QueueStatus.READY)
        )).thenReturn(Optional.of(existingQueue));
        when(queueRepository.countWaitingBefore(10L, 300L)).thenReturn(0L);

        // when
        QueueDto.EnterResponse response = queueService.enterQueue(1L, new QueueDto.EnterRequest(10L));

        // then
        verify(queueRepository).save(existingQueue);
        assertThat(existingQueue.getStatus()).isEqualTo(QueueStatus.READY);
        assertThat(response.queueId()).isEqualTo(300L);
        assertThat(response.position()).isZero();
    }

    private QueueService queueService() {
        QueueService queueService = new QueueService(queueRepository, showScheduleRepository, userRepository);
        ReflectionTestUtils.setField(queueService, "readyTtlMinutes", 5);
        return queueService;
    }

    private Queue queue(Long id) {
        Queue queue = Queue.builder()
                .user(mock(User.class))
                .schedule(mock(ShowSchedule.class))
                .build();
        ReflectionTestUtils.setField(queue, "id", id);
        return queue;
    }
}
