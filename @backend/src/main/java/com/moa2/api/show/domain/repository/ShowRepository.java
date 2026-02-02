package com.moa2.api.show.domain.repository;

import com.moa2.api.show.domain.entity.Show;
import com.moa2.global.model.Genre;
import com.moa2.global.model.Region;
import com.moa2.global.model.ShowStatus;
import com.moa2.global.model.SaleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;

@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {

        @Query("SELECT s FROM Show s " +
                        "WHERE (:showStatus IS NULL OR s.status = :showStatus) " +
                        "AND (:saleStatus IS NULL OR s.saleStatus = :saleStatus) " +
                        "AND (:genre IS NULL OR s.genre = :genre) " +
                        "AND (:keywordPattern IS NULL OR LOWER(s.title) LIKE :keywordPattern) " +
                        "AND (CAST(:startDate AS date) IS NULL OR s.startDate >= :startDate) " +
                        "AND (CAST(:endDate AS date) IS NULL OR s.endDate <= :endDate)")
        Page<Show> findShowsWithFilters(
                        @Param("showStatus") ShowStatus showStatus,
                        @Param("saleStatus") SaleStatus saleStatus,
                        @Param("genre") Genre genre,
                        @Param("keywordPattern") String keywordPattern,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        Pageable pageable);

        @Query("SELECT s FROM Show s WHERE s.id = :id")
        Show findByIdAndNotDeleted(@Param("id") Long id);

        /**
         * 사용자용 공연 목록 조회
         * - 판매 허용(ALLOWED)된 공연만
         * - 판매중(ON_SALE) 또는 매진(SOLD_OUT) 상태만 (TODO: 프로덕션에서는 WAITING 제외)
         * - 장르, 지역, 키워드, 날짜 필터 지원
         */
        @Query("SELECT s FROM Show s " +
                        "WHERE s.saleStatus = 'ALLOWED' " +
                        "AND s.status IN ('ON_SALE', 'SOLD_OUT') " + // 임시: WAITING 포함
                        "AND (:genre IS NULL OR s.genre = :genre) " +
                        "AND (:region IS NULL OR s.venue.region = :region) " +
                        "AND (:keyword IS NULL OR LOWER(s.title) LIKE :keyword) " +
                        "AND (CAST(:startDate AS date) IS NULL OR s.startDate >= :startDate) " +
                        "AND (CAST(:endDate AS date) IS NULL OR s.endDate <= :endDate)")
        Page<Show> findShowsForUser(
                        @Param("genre") Genre genre,
                        @Param("region") Region region,
                        @Param("keyword") String keyword,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        Pageable pageable);
}
