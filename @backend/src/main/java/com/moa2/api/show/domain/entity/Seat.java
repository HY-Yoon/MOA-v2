package com.moa2.api.show.domain.entity;

import com.moa2.api.show.domain.entity.Venue;
import com.moa2.api.show.domain.entity.VenueSeatSection;
import com.moa2.global.entity.BaseTimeEntity;
import com.moa2.global.model.SeatStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "seats")
public class Seat extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private VenueSeatSection section;

    private String seatRow; // 행
    private Integer seatNumber; // 번호

    @Enumerated(EnumType.STRING)
    private SeatStatus status; // AVAILABLE, LOCKED, RESERVED, SOLD

    @Column(name = "x_coord")
    private Integer x;

    @Column(name = "y_coord")
    private Integer y;
}
