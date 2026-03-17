package com.moa2.api.reservation.domain.specification;

import com.moa2.api.reservation.domain.entity.Payment;
import com.moa2.api.reservation.domain.entity.Reservation;
import com.moa2.api.show.domain.entity.ShowSchedule;
import com.moa2.api.user.domain.entity.User;
import com.moa2.global.model.PaymentStatus;
import com.moa2.global.model.ReservationStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ReservationSpecification {

    public static Specification<Reservation> equalReservationId(Long reservationId) {
        return (root, query, criteriaBuilder) -> {
            if (reservationId == null) return null;
            return criteriaBuilder.equal(root.get("id"), reservationId);
        };
    }

    public static Specification<Reservation> equalShowId(Long showId) {
        return (root, query, criteriaBuilder) -> {
            if (showId == null) return null;
            Join<Reservation, ShowSchedule> scheduleJoin = root.join("showSchedule", JoinType.INNER);
            return criteriaBuilder.equal(scheduleJoin.get("show").get("id"), showId);
        };
    }

    public static Specification<Reservation> equalScheduleId(Long scheduleId) {
        return (root, query, criteriaBuilder) -> {
            if (scheduleId == null) return null;
            return criteriaBuilder.equal(root.get("showSchedule").get("id"), scheduleId);
        };
    }

    public static Specification<Reservation> equalUser(User user) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("user"), user);
    }

    public static Specification<Reservation> equalStatus(String status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null || status.isEmpty()) {
                return null;
            }
            return criteriaBuilder.equal(root.get("status"), ReservationStatus.valueOf(status));
        };
    }

    public static Specification<Reservation> equalStatus(ReservationStatus status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) {
                return null;
            }
            return criteriaBuilder.equal(root.get("status"), status);
        };
    }

    public static Specification<Reservation> equalPaymentStatus(PaymentStatus status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) {
                return null;
            }
            // Payment와 조인 (LEFT JOIN을 사용하여 Payment가 없는 경우도 고려할지, 아니면 결제 상태 필터링 시에는 필수인지 결정)
            Join<Reservation, Payment> paymentJoin = root.join("payment", JoinType.LEFT);
            return criteriaBuilder.equal(paymentJoin.get("status"), status);
        };
    }

    public static Specification<Reservation> search(String keyword,
            com.moa2.api.reservation.dto.AdminReservationDto.ReservationSearchType type) {
        return (root, query, criteriaBuilder) -> {
            if (keyword == null || keyword.isEmpty()) {
                return null;
            }
            switch (type) {
                case RESERVATION_NUMBER:
                    return criteriaBuilder.like(root.get("reservationNumber"), "%" + keyword + "%");
                case BOOKER_NAME:
                    return criteriaBuilder.like(root.get("user").get("name"), "%" + keyword + "%");
                case BOOKER_ID:
                    return criteriaBuilder.like(root.get("user").get("email"), "%" + keyword + "%");
                case SHOW_TITLE:
                    Join<Reservation, ShowSchedule> scheduleJoin = root.join("showSchedule", JoinType.INNER);
                    return criteriaBuilder.like(scheduleJoin.get("show").get("title"), "%" + keyword + "%");
                default:
                    return null;
            }
        };
    }

    public static Specification<Reservation> betweenDate(String dateType, LocalDate startDate, LocalDate endDate) {
        return (root, query, criteriaBuilder) -> {
            if (startDate == null && endDate == null) {
                return null;
            }

            LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
            LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(23, 59, 59) : null;

            if ("SHOW".equalsIgnoreCase(dateType)) {
                return betweenShowDate(startDateTime, endDateTime).toPredicate(root, query, criteriaBuilder);
            } else {
                return betweenDate(startDateTime, endDateTime).toPredicate(root, query, criteriaBuilder);
            }
        };
    }

    public static Specification<Reservation> betweenDate(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, criteriaBuilder) -> {
            if (startDate == null && endDate == null) {
                return null;
            }
            if (startDate != null && endDate != null) {
                return criteriaBuilder.between(root.get("createdAt"), startDate, endDate);
            } else if (startDate != null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startDate);
            } else {
                return criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endDate);
            }
        };
    }

    public static Specification<Reservation> betweenShowDate(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, criteriaBuilder) -> {
            if (startDate == null && endDate == null) {
                return null;
            }
            Join<Reservation, ShowSchedule> scheduleJoin = root.join("showSchedule", JoinType.INNER);

            if (startDate != null && endDate != null) {
                return criteriaBuilder.between(scheduleJoin.get("showDate"), startDate.toLocalDate(),
                        endDate.toLocalDate());
            } else if (startDate != null) {
                return criteriaBuilder.greaterThanOrEqualTo(scheduleJoin.get("showDate"), startDate.toLocalDate());
            } else {
                return criteriaBuilder.lessThanOrEqualTo(scheduleJoin.get("showDate"), endDate.toLocalDate());
            }
        };
    }
}
