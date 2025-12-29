package com.moa2.domain.show.repository;

import com.moa2.domain.show.entity.SeatLayout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatLayoutRepository extends JpaRepository<SeatLayout, Long> {
    List<SeatLayout> findByVenueId(Long venueId);
    
    Optional<SeatLayout> findFirstByVenueIdOrderByIdDesc(Long venueId);
}

