package com.moa2.domain.show.repository;

import com.moa2.domain.show.entity.Seat;
import com.moa2.domain.show.entity.Venue;
import com.moa2.global.model.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    
    @Query("SELECT COUNT(s) FROM Seat s WHERE s.venue.id = :venueId")
    Long countByVenueId(@Param("venueId") Long venueId);
    
    @Query("SELECT COUNT(s) FROM Seat s WHERE s.venue.id = :venueId AND s.status = :status")
    Long countByVenueIdAndStatus(@Param("venueId") Long venueId, @Param("status") SeatStatus status);
}

