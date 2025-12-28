package com.moa2.entity.show;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "casts")
public class Cast {
    
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String profileImageUrl;
    private LocalDate birthDate;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

