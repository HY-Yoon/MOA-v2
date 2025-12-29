package com.moa2.domain.show.repository;

import com.moa2.domain.show.entity.ShowCastSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ShowCastScheduleRepository extends JpaRepository<ShowCastSchedule, Long> {
    List<ShowCastSchedule> findByShowCastId(Long showCastId);
    
    void deleteByShowCastId(Long showCastId);
    
    void deleteByShowCastShowId(Long showId);
}

