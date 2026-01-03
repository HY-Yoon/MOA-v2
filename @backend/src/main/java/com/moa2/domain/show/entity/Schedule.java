package com.moa2.domain.show.entity;

import com.moa2.global.model.ScheduleStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "schedule")
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id")
    private Show show;

    private LocalDate showDate;

    private LocalTime showTime;

    @Enumerated(EnumType.STRING)
    private ScheduleStatus status;
}







