package com.moa2.domain.show.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "show_cast_schedules", uniqueConstraints = {
    @UniqueConstraint(name = "uq_show_cast_schedule", columnNames = {"show_cast_id", "schedule_id"})
})
public class ShowCastSchedule {
    
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_cast_id", nullable = false)
    private ShowCast showCast;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private ShowSchedule schedule;
    
    public ShowCastSchedule(ShowCast showCast, ShowSchedule schedule) {
        this.showCast = showCast;
        this.schedule = schedule;
    }
}

