package com.moa2.domain.show.entity;

import com.moa2.global.model.Region;
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
@Table(name = "venues")
public class Venue {
    
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name; // 공연장명 (ex. 예술의전당)
    private String address;
    
    @Enumerated(EnumType.STRING)
    private Region region; // 지역

    private String hallName; // 홀명
    
    private Double latitude;
    private Double longitude;
    private Integer totalSeats;
    private String seatLayoutImageUrl;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
