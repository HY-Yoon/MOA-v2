package com.moa2.domain.show.repository;

import com.moa2.domain.show.entity.VenueSeatSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VenueSeatSectionRepository extends JpaRepository<VenueSeatSection, Long> {
    List<VenueSeatSection> findByVenueId(Long venueId);
}

