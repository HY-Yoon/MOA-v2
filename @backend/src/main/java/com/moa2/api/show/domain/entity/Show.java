package com.moa2.api.show.domain.entity;

import com.moa2.global.entity.BaseTimeEntity;
import com.moa2.global.model.Genre;
import com.moa2.global.model.ShowStatus;
import com.moa2.global.model.SaleStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "shows")
public class Show extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    private String title;

    @Enumerated(EnumType.STRING)
    private Genre genre; // MUSICAL, CONCERT...

    private String runningTime; // 상영 시간 (예: "150분", "2시간 30분")
    private String posterUrl;

    @Column(name = "\"cast\"")
    private String cast; // 출연진 정보 (단순 문자열)

    @OneToMany(mappedBy = "show", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private java.util.List<DetailImage> detailImages = new java.util.ArrayList<>();

    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private ShowStatus status;

    @Enumerated(EnumType.STRING)
    private SaleStatus saleStatus;

    private LocalDateTime saleStartDate; // 판매 시작일시
    private LocalDateTime saleEndDate; // 판매 종료일시

    private Long viewCount;
}
