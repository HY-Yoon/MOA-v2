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
import java.time.LocalTime;

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

    @OneToMany(mappedBy = "show", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<ShowSeatGrade> showSeatGrades = new java.util.ArrayList<>();

    private LocalTime startTime;
    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private ShowStatus status;

    @Enumerated(EnumType.STRING)
    private SaleStatus saleStatus;

    private LocalDateTime saleStartDate; // 판매 시작일시
    private LocalDateTime saleEndDate; // 판매 종료일시

    private Long viewCount;

    public void increaseViewCount() {
        if (this.viewCount == null) {
            this.viewCount = 0L;
        }
        this.viewCount++;
    }

    public void update(String title, String runningTime, String cast) {
        if (title != null)
            this.title = title;
        if (runningTime != null)
            this.runningTime = runningTime;
        if (cast != null)
            this.cast = cast;
    }

    public void updateWaitStatusFields(Genre genre, Venue venue, LocalDateTime saleStartDate,
            LocalDateTime saleEndDate) {
        if (this.status != ShowStatus.WAITING) {
            throw new IllegalStateException("WAITING 상태가 아닌 공연은 중요 정보를 수정할 수 없습니다.");
        }
        if (genre != null)
            this.genre = genre;
        if (venue != null)
            this.venue = venue;
        if (saleStartDate != null)
            this.saleStartDate = saleStartDate;
        if (saleEndDate != null)
            this.saleEndDate = saleEndDate;
    }

    public void updatePoster(String posterUrl) {
        this.posterUrl = posterUrl;
    }
}