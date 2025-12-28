package com.moa2.domain.show.entity;

import com.moa2.global.model.Genre;
import com.moa2.global.model.ShowStatus;
import com.moa2.global.model.SaleStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "shows")
public class Show {
    
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    private String title;
    
    @Enumerated(EnumType.STRING)
    private Genre genre; // MUSICAL, CONCERT...
    
    private Integer runningTime;
    private String posterUrl;
    
    // PostgreSQL Array 타입 처리 필요 (혹은 별도 테이블 분리 가능하나 단순화를 위해 문자열 가정)
    // @Type(ListArrayType.class) 
    // private List<String> detailImageUrls;
    @Column(columnDefinition = "TEXT[]")
    private String[] detailImageUrls;

    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private ShowStatus status;

    @Enumerated(EnumType.STRING)
    private SaleStatus saleStatus;

    private Long viewCount;
}

