package com.moa2.api.queue;

import com.moa2.api.queue.scheduler.QueueScheduler;
import com.moa2.api.queue.queue.entity.Queue;
import com.moa2.api.queue.queue.repository.QueueRepository;
import com.moa2.api.show.domain.entity.Show;
import com.moa2.api.show.domain.entity.ShowSchedule;
import com.moa2.api.show.domain.repository.ShowRepository;
import com.moa2.api.show.domain.repository.ShowScheduleRepository;
import com.moa2.api.user.domain.entity.User;
import com.moa2.api.user.domain.repository.UserRepository;
import com.moa2.global.model.Genre;
import com.moa2.global.model.QueueStatus;
import com.moa2.global.model.SaleStatus;
import com.moa2.global.model.ScheduleStatus;
import com.moa2.global.model.ShowStatus;
import com.moa2.global.model.SocialProvider;
import com.moa2.global.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class QueueGateFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private QueueScheduler queueScheduler;

    @Autowired private UserRepository userRepository;
    @Autowired private ShowRepository showRepository;
    @Autowired private ShowScheduleRepository showScheduleRepository;
    @Autowired private QueueRepository queueRepository;

    @BeforeEach
    void setUp() {
        // Cleanup order matters because of FK relations.
        queueRepository.deleteAll();
        showScheduleRepository.deleteAll();
        showRepository.deleteAll();
        userRepository.deleteAll();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private RequestPostProcessor withAuth(Authentication auth) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(auth);
            return request;
        };
    }

    @Test
    void e2e_queue_waiting_blocks_seats_scheduler_promotes_ready_allows_seats() throws Exception {
        // given: a real user + show + schedule
        User user = userRepository.saveAndFlush(User.builder()
                .email("queue-test@moa2.com")
                .socialProvider(SocialProvider.GOOGLE)
                .providerId("google-123")
                .name("queue-test-user")
                .build());

        Show show = showRepository.saveAndFlush(Show.builder()
                .title("Queue Test Show")
                .genre(Genre.MUSICAL)
                .status(ShowStatus.WAITING)
                .saleStatus(SaleStatus.ALLOWED)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(1))
                .build());

        ShowSchedule schedule = showScheduleRepository.saveAndFlush(ShowSchedule.builder()
                .show(show)
                .showDate(LocalDate.now())
                .showTime(LocalTime.of(19, 30))
                .ticketOpenTime(LocalDateTime.now().minusDays(1))
                .status(ScheduleStatus.OPEN)
                .build());

        Authentication auth = new UsernamePasswordAuthenticationToken(
                new UserPrincipal(user.getEmail(), user.getSocialProvider().name()),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // 1) 진입: POST /tokens 호출 -> DB 상 WAITING 인가?
        mockMvc.perform(
                        post("/api/v1/queue/tokens")
                                .with(withAuth(auth))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"scheduleId\":\"" + schedule.getId() + "\"}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.queueId").isNumber());

        Queue queueAfterEnter = queueRepository.findTopByUserIdAndScheduleIdOrderByCreatedAtDesc(user.getId(), schedule.getId())
                .orElseThrow();
        assertThat(queueAfterEnter.getStatus()).isEqualTo(QueueStatus.WAITING);

        // 2) 차단: WAITING일 때 좌석 조회가 403으로 막히는가?
        mockMvc.perform(
                        get("/api/v1/schedules/{scheduleId}/seats", schedule.getId())
                                .with(withAuth(auth))
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        // 3) 승격: 스케줄러(processQueue)를 강제로 돌리면 READY로 변하는가?
        queueScheduler.processQueue();

        Queue queueAfterScheduler = queueRepository.findTopByUserIdAndScheduleIdOrderByCreatedAtDesc(user.getId(), schedule.getId())
                .orElseThrow();
        assertThat(queueAfterScheduler.getStatus()).isEqualTo(QueueStatus.READY);
        assertThat(queueAfterScheduler.getActiveUntil()).isNotNull();

        // 4) 통과: READY일 때 좌석 조회가 200으로 통과되는가?
        mockMvc.perform(
                        get("/api/v1/schedules/{scheduleId}/seats", schedule.getId())
                                .with(withAuth(auth))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scheduleId").value(schedule.getId().intValue()));
    }
}

