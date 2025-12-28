package com.moa2.entity.venue;

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
    private Double latitude;
    private Double longitude;
    private Integer totalSeats;
    private String seatLayoutImageUrl;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
