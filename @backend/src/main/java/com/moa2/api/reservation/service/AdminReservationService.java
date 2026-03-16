package com.moa2.api.reservation.service;

import com.moa2.api.reservation.dto.AdminReservationDto;
import com.moa2.api.reservation.domain.entity.Reservation;
import com.moa2.api.reservation.domain.entity.ReservationSeat;
import com.moa2.api.reservation.domain.repository.ReservationRepository;
import com.moa2.api.reservation.domain.repository.ReservationSeatRepository;
import com.moa2.api.reservation.domain.specification.ReservationSpecification;
import com.moa2.global.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminReservationService {

        private final ReservationRepository reservationRepository;
        private final ReservationSeatRepository reservationSeatRepository;

        /**
         * 관리자용 모든 예매 내역 조회 (검색/필터링 적용)
         */
        @Transactional(readOnly = true)
        public PageResponse<AdminReservationDto.ListResponse> getReservations(
                        AdminReservationDto.SearchCondition condition,
                        Pageable pageable) {

                Specification<Reservation> spec = Specification.where(null);

                if (condition.status() != null) {
                        spec = spec.and(ReservationSpecification.equalStatus(condition.status()));
                }
                if (condition.paymentStatus() != null) {
                        spec = spec.and(ReservationSpecification.equalPaymentStatus(condition.paymentStatus()));
                }
                if (condition.searchKeyword() != null && !condition.searchKeyword().isBlank()) {
                        spec = spec.and(ReservationSpecification.search(condition.searchKeyword(), condition.searchType()));
                }
                if (condition.showId() != null) {
                        spec = spec.and(ReservationSpecification.equalShowId(condition.showId()));
                }
                if (condition.scheduleId() != null) {
                        spec = spec.and(ReservationSpecification.equalScheduleId(condition.scheduleId()));
                }
                if (condition.startDate() != null || condition.endDate() != null) {
                        // DateSearchType에 따라 날짜 검색 조건 분기
                        if (condition.dateSearchType() == AdminReservationDto.DateSearchType.SHOW_DATE) {
                                spec = spec.and(ReservationSpecification.betweenShowDate(condition.startDate(), condition.endDate()));
                        } else {
                                // Default: RESERVATION_DATE
                                spec = spec.and(ReservationSpecification.betweenDate(condition.startDate(), condition.endDate()));
                        }
                }

                // @EntityGraph가 적용된 findAll 사용 (N+1 방지)
                Page<Reservation> reservations = reservationRepository.findAll(spec, pageable);

                // Entity -> DTO 변환 (Factory Method 사용)
                Page<AdminReservationDto.ListResponse> responseList = reservations
                                .map(AdminReservationDto.ListResponse::from);

                return PageResponse.of(responseList);
        }

        /**
         * 관리자용 예매 상세 조회 (reservationId 단건 필터)
         */
        @Transactional(readOnly = true)
        public PageResponse<AdminReservationDto.DetailResponse> getReservationDetails(
                        Long reservationId) {

                Specification<Reservation> spec = Specification.where(
                                ReservationSpecification.equalReservationId(reservationId));
                Pageable pageable = PageRequest.of(0, 1);

                // EntityGraph 적용된 findAll로 연관 데이터 한번에 로드 (User, ShowSchedule, Show, Venue, Payment)
                Page<Reservation> reservations = reservationRepository.findAll(spec, pageable);

                // 각 예매별 좌석 정보 조회 후 DetailResponse 변환
                Page<AdminReservationDto.DetailResponse> details = reservations.map(reservation -> {
                        List<ReservationSeat> seats = reservationSeatRepository
                                        .findByReservationWithSeat(reservation);
                        return AdminReservationDto.DetailResponse.from(reservation, seats);
                });

                return PageResponse.of(details);
        }
}
