package com.moa2.domain.show.repository;

import com.moa2.domain.show.entity.ShowCast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ShowCastRepository extends JpaRepository<ShowCast, Long> {
    List<ShowCast> findByShowId(Long showId);
    
    void deleteByShowId(Long showId);
}

