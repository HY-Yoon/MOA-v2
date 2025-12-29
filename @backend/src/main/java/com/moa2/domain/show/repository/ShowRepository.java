package com.moa2.domain.show.repository;

import com.moa2.domain.show.entity.Show;
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
           "AND (:keyword IS NULL OR s.title LIKE CONCAT('%', :keyword, '%')) " +
           "AND (:startDate IS NULL OR s.startDate >= :startDate) " +
           "AND (:endDate IS NULL OR s.endDate <= :endDate) " +
           "AND s.status != 'SUSPENDED'")
    Page<Show> findShowsWithFilters(
        @Param("showStatus") ShowStatus showStatus,
        @Param("saleStatus") SaleStatus saleStatus,
        @Param("keyword") String keyword,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        Pageable pageable
    );
    
    @Query("SELECT s FROM Show s WHERE s.id = :id AND s.status != 'SUSPENDED'")
    Show findByIdAndNotDeleted(@Param("id") Long id);
}

