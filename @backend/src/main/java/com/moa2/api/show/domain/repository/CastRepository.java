package com.moa2.api.show.domain.repository;

import com.moa2.api.show.domain.entity.Cast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CastRepository extends JpaRepository<Cast, Long> {
}

