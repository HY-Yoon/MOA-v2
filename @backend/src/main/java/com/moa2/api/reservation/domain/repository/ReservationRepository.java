package com.moa2.api.reservation.domain.repository;

import com.moa2.api.reservation.domain.entity.Reservation;
import com.moa2.api.user.domain.entity.User;
import com.moa2.global.model.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

       @Query("SELECT COUNT(r) FROM Reservation r WHERE r.showSchedule.id = :scheduleId AND r.status != 'CANCELLED'")
       Long countByScheduleId(@Param("scheduleId") Long scheduleId);

       @Query("SELECT r.showSchedule.id, COUNT(r) FROM Reservation r WHERE r.showSchedule.id IN :scheduleIds AND r.status != 'CANCELLED' GROUP BY r.showSchedule.id")
       List<Object[]> countReservationsByScheduleIds(@Param("scheduleIds") List<Long> scheduleIds);

       @Query("SELECT r FROM Reservation r JOIN FETCH r.showSchedule sch JOIN FETCH sch.show WHERE r.reservationNumber = :reservationNumber")
       Optional<Reservation> findByReservationNumberWithSchedule(@Param("reservationNumber") String reservationNumber);

       /**
        * 사용자의 특정 상태 예매 목록 조회 (진행중인 예매 확인용)
        */
       List<Reservation> findByUserAndStatusIn(User user, List<ReservationStatus> statuses);

       /**
        * 사용자의 모든 예매 목록 조회 (페이지네이션)
        */
       /**
        * 사용자의 모든 예매 목록 조회 (페이지네이션) + N+1 방지
        * Payment는 OneToOne lazy 로딩 이슈로 필요 시 fetch join (여기선 ListResponse에
        * paymentStatus가 필요하므로 포함)
        */
       @Query("SELECT r FROM Reservation r " +
                     "JOIN FETCH r.showSchedule sch " +
                     "JOIN FETCH sch.show s " +
                     "JOIN FETCH s.venue v " +
                     "LEFT JOIN FETCH r.payment p " +
                     "WHERE r.user = :user " +
                     "ORDER BY r.createdAt DESC")
       Page<Reservation> findWithDetailsByUser(@Param("user") User user, Pageable pageable);

       /**
        * 사용자의 특정 상태 예매 목록 조회 (페이지네이션) + N+1 방지
        */
       @Query("SELECT r FROM Reservation r " +
                     "JOIN FETCH r.showSchedule sch " +
                     "JOIN FETCH sch.show s " +
                     "JOIN FETCH s.venue v " +
                     "LEFT JOIN FETCH r.payment p " +
                     "WHERE r.user = :user AND r.status = :status " +
                     "ORDER BY r.createdAt DESC")
       Page<Reservation> findWithDetailsByUserAndStatus(@Param("user") User user,
                     @Param("status") ReservationStatus status,
                     Pageable pageable);

       /**
        * 예매 상세 조회 (사용자 검증용) + N+1 방지
        */
       @Query("SELECT r FROM Reservation r " +
                     "JOIN FETCH r.showSchedule sch " +
                     "JOIN FETCH sch.show s " +
                     "JOIN FETCH s.venue v " +
                     "LEFT JOIN FETCH r.payment p " +
                     "WHERE r.id = :reservationId AND r.user = :user")
       Optional<Reservation> findWithDetailsByIdAndUser(@Param("reservationId") Long reservationId,
                     @Param("user") User user);

       /**
        * [Admin] 예매 상세 조회 (User, Schedule, Show, Venue, Payment Fetch Join)
        */
       @Query("SELECT r FROM Reservation r " +
                     "JOIN FETCH r.user u " +
                     "JOIN FETCH r.showSchedule sch " +
                     "JOIN FETCH sch.show s " +
                     "JOIN FETCH s.venue v " +
                     "LEFT JOIN FETCH r.payment p " +
                     "WHERE r.id = :reservationId")
       Optional<Reservation> findByIdWithDetails(@Param("reservationId") Long reservationId);

       Optional<Reservation> findByIdAndUser(Long id, User user);

       /**
        * [Admin] Specification + EntityGraph (N+1 방지)
        * attributePaths에 연관관계 명시.
        * 주의: ToOne 관계는 기본적으로 Eager Fetching 되기도 하지만, EntityGraph로 명시하면 확실하게 한 번의 쿼리로
        * 가져옴.
        * ToMany 관계는 페이징 시 fetch join 하면 안되지만, 여기선 ToOne 관계들 위주로 설정.
        * ReservationSeat(좌석)은 List(ToMany)이므로 목록 조회시엔 fetch join 하지 않음 (필요하면
        * BatchSize로 해결).
        */
       @Override
       @EntityGraph(attributePaths = { "user", "showSchedule", "showSchedule.show", "showSchedule.show.venue",
                     "payment" })
       Page<Reservation> findAll(Specification<Reservation> spec, Pageable pageable);
}
