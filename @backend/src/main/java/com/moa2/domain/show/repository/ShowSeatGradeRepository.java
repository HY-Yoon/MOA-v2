package com.moa2.domain.show.repository;

import com.moa2.domain.show.entity.ShowSeatGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ShowSeatGradeRepository extends JpaRepository<ShowSeatGrade, Long> {
    List<ShowSeatGrade> findByShowId(Long showId);
    
    void deleteByShowId(Long showId);
}

