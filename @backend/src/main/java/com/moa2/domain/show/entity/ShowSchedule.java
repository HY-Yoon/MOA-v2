package com.moa2.domain.show.entity;

import com.moa2.global.model.ScheduleStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "show_schedules")
public class ShowSchedule {
    
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // [단방향] 스케줄 -> 공연
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    private LocalDate showDate;
    private LocalTime showTime;
    private LocalDateTime ticketOpenTime;
    
    @Enumerated(EnumType.STRING)
    private ScheduleStatus status;
}

