package com.moa2.api.show.domain.repository;

import com.moa2.api.show.domain.entity.ShowCast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ShowCastRepository extends JpaRepository<ShowCast, Long> {
    List<ShowCast> findByShowId(Long showId);
    
    void deleteByShowId(Long showId);
}

