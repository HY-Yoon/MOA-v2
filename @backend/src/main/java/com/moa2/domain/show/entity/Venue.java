package com.moa2.domain.show.entity;

import com.moa2.global.model.Region;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "venues")
public class Venue {
    
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String address;
    
    @Enumerated(EnumType.STRING)
    private Region region;
    
    private Double latitude;
    private Double longitude;
    private Integer totalSeats;
    private String seatLayoutImageUrl;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

