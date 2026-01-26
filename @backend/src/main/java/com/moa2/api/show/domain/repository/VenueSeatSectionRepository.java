package com.moa2.api.show.domain.repository;

import com.moa2.api.show.domain.entity.VenueSeatSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VenueSeatSectionRepository extends JpaRepository<VenueSeatSection, Long> {
    List<VenueSeatSection> findByVenueId(Long venueId);
}

