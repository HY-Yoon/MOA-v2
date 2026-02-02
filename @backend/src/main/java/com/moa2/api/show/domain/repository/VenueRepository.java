package com.moa2.api.show.domain.repository;

import com.moa2.api.show.domain.entity.Venue;
import com.moa2.global.model.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VenueRepository extends JpaRepository<Venue, Long> {
    Optional<Venue> findByNameAndHallNameAndRegion(String name, String hallName, Region region);
}

