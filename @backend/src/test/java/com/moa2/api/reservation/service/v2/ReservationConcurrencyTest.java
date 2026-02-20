package com.moa2.api.reservation.service.v2;

import com.moa2.api.reservation.domain.entity.Reservation;
import com.moa2.api.reservation.domain.repository.ReservationRepository;
import com.moa2.api.show.domain.entity.*;
import com.moa2.api.show.domain.repository.*;
import com.moa2.api.user.domain.entity.User;
import com.moa2.api.user.domain.repository.UserRepository;
import com.moa2.global.model.SeatStatus;
import com.moa2.global.model.ShowStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
@DisplayName("V2 예약 서비스 동시성 테스트")
class ReservationConcurrencyTest {

    @Autowired
    private ReservationServiceV2 reservationServiceV2;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ShowRepository showRepository;
    @Autowired
    private ShowScheduleRepository showScheduleRepository;
    @Autowired
    private VenueRepository venueRepository;
    @Autowired
    private VenueSeatSectionRepository venueSeatSectionRepository;
    @Autowired
    private SeatRepository seatRepository;
    @Autowired
    private ShowSeatGradeRepository showSeatGradeRepository;
    @Autowired
    private ScheduleSeatRepository scheduleSeatRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // ShedLock이 테스트 환경에서 동작하려면 LockProvider가 필요한데,
    // 통합 테스트 시 빈 충돌을 방지하거나 실제 Redis를 쓰도록 둠.
    // 여기서는 별도 설정 없이 진행 (LockProvider는 ShedLockConfig에서 등록됨)

    private Long scheduleId;
    private Long seatId;
    private List<Long> userIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // 1. 데이터 정리
        reservationRepository.deleteAll();
        scheduleSeatRepository.deleteAll();
        showSeatGradeRepository.deleteAll();
        showScheduleRepository.deleteAll();
        showRepository.deleteAll();
        seatRepository.deleteAll();
        venueSeatSectionRepository.deleteAll();
        venueRepository.deleteAll();
        userRepository.deleteAll();

        // Redis 정리
        if (redisTemplate.getConnectionFactory() != null) {
            redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        }

        // 2. 공연장(Venue) 생성
        Venue venue = Venue.builder()
                .name("LG아트센터")
                .address("서울 강서구")
                .totalSeats(1000)
                .build();
        venueRepository.save(venue);

        // 3. 구역(Section) 생성
        VenueSeatSection section = VenueSeatSection.builder()
                .venue(venue)
                .name("VIP")
                .defaultPrice(150000)
                .build();
        venueSeatSectionRepository.save(section);

        // 4. 좌석(Seat) 생성
        Seat seat = Seat.builder()
                .venue(venue)
                .section(section)
                .seatRow("A")
                .seatNumber(1)
                .status(SeatStatus.AVAILABLE)
                .x(10)
                .y(10)
                .build();
        seatRepository.save(seat);

        // 5. 공연(Show) 생성
        Show show = Show.builder()
                .venue(venue)
                .title("오페라의 유령")
                .runningTime("150분")
                .status(ShowStatus.ON_SALE)
                .build();
        showRepository.save(show);

        // 6. 스케줄(ShowSchedule) 생성
        ShowSchedule schedule = ShowSchedule.builder()
                .show(show)
                .showDate(LocalDate.now().plusDays(7))
                .showTime(LocalTime.of(19, 30))
                .build();
        showScheduleRepository.save(schedule);
        this.scheduleId = schedule.getId();

        // 7. 등급(ShowSeatGrade) 생성
        ShowSeatGrade grade = ShowSeatGrade.builder()
                .show(show)
                .section(section)
                .price(150000)
                .build();
        showSeatGradeRepository.save(grade);

        // 8. 스케줄 좌석(ScheduleSeat) 생성
        ScheduleSeat scheduleSeat = ScheduleSeat.builder()
                .schedule(schedule)
                .seat(seat)
                .grade(grade)
                .build();
        scheduleSeatRepository.save(scheduleSeat);
        this.seatId = scheduleSeat.getId();

        // 9. 사용자(User) 100명 생성
        userIds.clear();
        for (int i = 1; i <= 100; i++) {
            User user = User.builder()
                    .email("tester" + i + "@example.com")
                    .name("Tester" + i)
                    .socialProvider(com.moa2.global.model.SocialProvider.GOOGLE)
                    .build();
            userRepository.save(user);
            userIds.add(user.getId());
        }
    }

    @Test
    @DisplayName("동시에 100명의 서로 다른 유저가 같은 좌석을 예매하면 1명만 성공해야 한다")
    void concurrencyTest() throws InterruptedException {
        // given
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // when
        for (int i = 0; i < threadCount; i++) {
            Long userId = userIds.get(i); // 각 스레드마다 다른 User ID 사용

            executorService.submit(() -> {
                try {
                    reservationServiceV2.reserve(
                            userId,
                            scheduleId,
                            List.of(seatId),
                            "Booker",
                            "010-1234-5678",
                            "booker@test.com");
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 예매 실패 (락 획득 실패 or 이미 예매됨)
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // then
        System.out.println("성공 횟수: " + successCount.get());
        System.out.println("실패 횟수: " + failCount.get());

        // 1. 성공은 딱 1명이어야 함
        assertThat(successCount.get()).isEqualTo(1);

        // 2. 실패는 99명이어야 함
        assertThat(failCount.get()).isEqualTo(threadCount - 1);

        // 3. DB에 예약은 1개만 생성되어야 함
        List<Reservation> reservations = reservationRepository.findAll();
        assertThat(reservations).hasSize(1);

        // 4. 스케줄 좌석 상태가 변했는지 확인 (LOCKED or RESERVED according to logic)
        ScheduleSeat updatedSeat = scheduleSeatRepository.findById(seatId).orElseThrow();
        // 서비스 로직상 reserve() 호출 시 선점+예약 생성이므로 RESERVED 혹은 LOCKED 이어야 함.
        // 구현 로직: scheduleSeat.lock() -> LOCKED, scheduleSeat.reserve() -> RESERVED
        // 최종적으로 RESERVED 상태가 됨.
        assertThat(updatedSeat.getStatus()).isIn(SeatStatus.LOCKED, SeatStatus.RESERVED);
    }
}
