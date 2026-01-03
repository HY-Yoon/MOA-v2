package com.moa2.domain.show.repository;

import com.moa2.domain.show.entity.Venue;
import com.moa2.global.model.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VenueRepository extends JpaRepository<Venue, Long> {
    Optional<Venue> findByNameAndHallNameAndRegion(String name, String hallName, Region region);
}

