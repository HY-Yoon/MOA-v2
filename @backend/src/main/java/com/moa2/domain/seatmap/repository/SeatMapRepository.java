package com.moa2.domain.seatmap.repository;

import com.moa2.domain.seatmap.entity.SeatMap;
import com.moa2.global.model.Region;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SeatMapRepository extends JpaRepository<SeatMap, Long> {
    
    /**
     * region, venueName, hallName으로 중복 검사
     */
    Optional<SeatMap> findByRegionAndVenueNameAndHallName(Region region, String venueName, String hallName);
    
    /**
     * 필터링된 목록 조회 (페이징)
     */
    @Query("SELECT sm FROM SeatMap sm WHERE " +
           "(:region IS NULL OR sm.region = :region) AND " +
           "(:venueName IS NULL OR sm.venueName LIKE %:venueName%) AND " +
           "(:hallName IS NULL OR sm.hallName LIKE %:hallName%)")
    Page<SeatMap> findByFilters(
        @Param("region") Region region,
        @Param("venueName") String venueName,
        @Param("hallName") String hallName,
        Pageable pageable
    );
}

