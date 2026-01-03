package com.moa2.domain.show.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "venue_seat_sections")
public class VenueSeatSection {
    
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;
    
    private String name; // 구역명 (A구역, VIP구역 등)
    private Integer displayOrder; // 화면 노출 순서
    private Integer defaultPrice; // 기본 가격 (좌석배치도 등록 시 설정, 공연 등록 시 ShowSeatGrade로 사용)
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
