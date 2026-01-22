package com.moa2.api.schedule;

import com.moa2.api.queue.scheduler.QueueScheduler;
import com.moa2.api.schedule.scheduler.ScheduleSeatLockScheduler;
import com.moa2.domain.queue.repository.QueueRepository;
import com.moa2.domain.show.entity.ScheduleSeat;
import com.moa2.domain.show.entity.Seat;
import com.moa2.domain.show.entity.Show;
import com.moa2.domain.show.entity.ShowSchedule;
import com.moa2.domain.show.entity.ShowSeatGrade;
import com.moa2.domain.show.entity.Venue;
import com.moa2.domain.show.entity.VenueSeatSection;
import com.moa2.domain.show.repository.ScheduleSeatRepository;
import com.moa2.domain.show.repository.SeatRepository;
import com.moa2.domain.show.repository.ShowRepository;
import com.moa2.domain.show.repository.ShowScheduleRepository;
import com.moa2.domain.show.repository.ShowSeatGradeRepository;
import com.moa2.domain.show.repository.VenueRepository;
import com.moa2.domain.show.repository.VenueSeatSectionRepository;
import com.moa2.domain.user.entity.User;
import com.moa2.domain.user.repository.UserRepository;
import com.moa2.global.model.Genre;
import com.moa2.global.model.Region;
import com.moa2.global.model.SaleStatus;
import com.moa2.global.model.ScheduleStatus;
import com.moa2.global.model.SeatStatus;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Step4(좌석 선점) 테스트
 * - Queue READY 검증(Interceptor) + 좌석 선점(LOCK) + 409 충돌 + 만료 해제 스케줄러까지 확인
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ScheduleSeatLockFlowTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private QueueScheduler queueScheduler;
    @Autowired private ScheduleSeatLockScheduler scheduleSeatLockScheduler;

    @Autowired private UserRepository userRepository;
    @Autowired private ShowRepository showRepository;
    @Autowired private ShowScheduleRepository showScheduleRepository;
    @Autowired private QueueRepository queueRepository;

    @Autowired private VenueRepository venueRepository;
    @Autowired private VenueSeatSectionRepository venueSeatSectionRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private ShowSeatGradeRepository showSeatGradeRepository;
    @Autowired private ScheduleSeatRepository scheduleSeatRepository;

    @BeforeEach
    void setUp() {
        // Cleanup order matters because of FK relations.
        scheduleSeatRepository.deleteAll();
        queueRepository.deleteAll();
        showScheduleRepository.deleteAll();
        showSeatGradeRepository.deleteAll();
        seatRepository.deleteAll();
        venueSeatSectionRepository.deleteAll();
        venueRepository.deleteAll();
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
    void e2e_seat_lock_waiting_forbidden_ready_success_and_conflict() throws Exception {
        // given: user + schedule + scheduleSeats(AVAILABLE)
        User user = userRepository.saveAndFlush(User.builder()
                .email("seat-lock-test@moa2.com")
                .socialProvider(SocialProvider.GOOGLE)
                .providerId("google-999")
                .name("seat-lock-test-user")
                .build());

        Authentication auth = new UsernamePasswordAuthenticationToken(
                new UserPrincipal(user.getEmail(), user.getSocialProvider().name()),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        Venue venue = venueRepository.saveAndFlush(Venue.builder()
                .name("테스트 공연장")
                .hallName("테스트 홀")
                .address("테스트 주소")
                .region(Region.SEOUL)
                .totalSeats(100)
                .build());

        VenueSeatSection section = venueSeatSectionRepository.saveAndFlush(VenueSeatSection.builder()
                .venue(venue)
                .name("VIP")
                .displayOrder(1)
                .defaultPrice(150000)
                .build());

        Seat seat1 = seatRepository.saveAndFlush(newSeat(venue, section, "A", 1));
        Seat seat2 = seatRepository.saveAndFlush(newSeat(venue, section, "A", 2));

        Show show = showRepository.saveAndFlush(Show.builder()
                .title("Seat Lock Test Show")
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

        ShowSeatGrade grade = showSeatGradeRepository.saveAndFlush(ShowSeatGrade.builder()
                .show(show)
                .section(section)
                .price(150000)
                .build());

        scheduleSeatRepository.saveAllAndFlush(List.of(
                ScheduleSeat.builder().schedule(schedule).seat(seat1).grade(grade).build(),
                ScheduleSeat.builder().schedule(schedule).seat(seat2).grade(grade).build()
        ));

        // 1) 대기열 진입 -> 기본은 WAITING
        mockMvc.perform(
                        post("/api/v1/queue/tokens")
                                .with(withAuth(auth))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"scheduleId\":" + schedule.getId() + "}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 2) WAITING이면 좌석 선점은 403으로 막혀야 함
        mockMvc.perform(
                        post("/api/v1/schedules/{scheduleId}/seats/lock", schedule.getId())
                                .with(withAuth(auth))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"seatIds\":[" + seat1.getId() + "," + seat2.getId() + "]}")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        // 3) 스케줄러로 READY 승격
        queueScheduler.processQueue();

        // 4) READY면 선점 성공(200) + expiresAt 반환
        mockMvc.perform(
                        post("/api/v1/schedules/{scheduleId}/seats/lock", schedule.getId())
                                .with(withAuth(auth))
                                .contentType(MediaType.APPLICATION_JSON)
                                // 일부러 내림차순으로 보내도, 서비스에서 오름차순 정렬 후 락을 잡아야 함
                                .content("{\"seatIds\":[" + seat2.getId() + "," + seat1.getId() + "]}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isSuccess").value(true))
                .andExpect(jsonPath("$.data.expiresAt").isString());

        List<ScheduleSeat> lockedSeats = scheduleSeatRepository.findByScheduleId(schedule.getId());
        assertThat(lockedSeats).hasSize(2);
        assertThat(lockedSeats).allMatch(ss -> ss.getStatus() == SeatStatus.LOCKED);
        assertThat(lockedSeats).allMatch(ss -> user.getId().equals(ss.getLockedByUserId()));
        assertThat(lockedSeats).allMatch(ss -> ss.getLockedUntil() != null);

        // 5) 이미 LOCKED면 409 Conflict
        mockMvc.perform(
                        post("/api/v1/schedules/{scheduleId}/seats/lock", schedule.getId())
                                .with(withAuth(auth))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"seatIds\":[" + seat1.getId() + "]}")
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));

        // 6) 결제 취소 시나리오: 선점 해제(UNLOCK) 호출 -> AVAILABLE로 복구되는가?
        mockMvc.perform(
                        post("/api/v1/schedules/{scheduleId}/seats/unlock", schedule.getId())
                                .with(withAuth(auth))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"seatIds\":[" + seat1.getId() + "," + seat2.getId() + "]}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isSuccess").value(true));

        List<ScheduleSeat> unlockedSeats = scheduleSeatRepository.findByScheduleId(schedule.getId());
        assertThat(unlockedSeats).hasSize(2);
        assertThat(unlockedSeats).allMatch(ss -> ss.getStatus() == SeatStatus.AVAILABLE);
        assertThat(unlockedSeats).allMatch(ss -> ss.getLockedByUserId() == null);
        assertThat(unlockedSeats).allMatch(ss -> ss.getLockedUntil() == null);

        // 7) 언락 후 다시 선점 가능해야 함
        mockMvc.perform(
                        post("/api/v1/schedules/{scheduleId}/seats/lock", schedule.getId())
                                .with(withAuth(auth))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"seatIds\":[" + seat1.getId() + "]}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isSuccess").value(true));
    }

    @Test
    void scheduler_releases_expired_locks() {
        // given: 만료된 LOCKED 좌석 1개
        User user = userRepository.saveAndFlush(User.builder()
                .email("seat-lock-expire@moa2.com")
                .socialProvider(SocialProvider.GOOGLE)
                .providerId("google-111")
                .name("seat-lock-expire-user")
                .build());

        Venue venue = venueRepository.saveAndFlush(Venue.builder()
                .name("만료 테스트 공연장")
                .hallName("만료 테스트 홀")
                .address("테스트 주소")
                .region(Region.SEOUL)
                .totalSeats(10)
                .build());

        VenueSeatSection section = venueSeatSectionRepository.saveAndFlush(VenueSeatSection.builder()
                .venue(venue)
                .name("A구역")
                .displayOrder(1)
                .defaultPrice(100000)
                .build());

        Seat seat = seatRepository.saveAndFlush(newSeat(venue, section, "B", 1));

        Show show = showRepository.saveAndFlush(Show.builder()
                .title("Expire Seat Lock Test Show")
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

        ShowSeatGrade grade = showSeatGradeRepository.saveAndFlush(ShowSeatGrade.builder()
                .show(show)
                .section(section)
                .price(100000)
                .build());

        ScheduleSeat scheduleSeat = scheduleSeatRepository.saveAndFlush(
                ScheduleSeat.builder().schedule(schedule).seat(seat).grade(grade).build()
        );

        LocalDateTime expiredUntil = LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusMinutes(1).withNano(0);
        scheduleSeat.lock(user.getId(), expiredUntil);
        scheduleSeatRepository.saveAndFlush(scheduleSeat);

        // when: 스케줄러 실행
        scheduleSeatLockScheduler.releaseExpiredSeatLocks();

        // then: AVAILABLE로 복구 + lock 정보 초기화
        ScheduleSeat refreshed = scheduleSeatRepository.findById(scheduleSeat.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(refreshed.getLockedByUserId()).isNull();
        assertThat(refreshed.getLockedUntil()).isNull();
    }

    /**
     * Seat 엔티티는 builder/setter가 없어서 테스트에서만 리플렉션으로 생성한다.
     */
    private Seat newSeat(Venue venue, VenueSeatSection section, String row, int number) {
        try {
            Constructor<Seat> ctor = Seat.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            Seat seat = ctor.newInstance();

            ReflectionTestUtils.setField(seat, "venue", venue);
            ReflectionTestUtils.setField(seat, "section", section);
            ReflectionTestUtils.setField(seat, "seatRow", row);
            ReflectionTestUtils.setField(seat, "seatNumber", number);
            ReflectionTestUtils.setField(seat, "status", SeatStatus.AVAILABLE);

            return seat;
        } catch (Exception e) {
            throw new IllegalStateException("Seat 테스트 데이터 생성 실패", e);
        }
    }
}

