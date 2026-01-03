package com.moa2.domain.show.entity;

import com.moa2.global.model.SeatStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "seats")
public class Seat {
    
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}







