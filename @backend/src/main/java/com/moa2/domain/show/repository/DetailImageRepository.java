package com.moa2.domain.show.repository;

import com.moa2.domain.show.entity.DetailImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetailImageRepository extends JpaRepository<DetailImage, Long> {
    
    List<DetailImage> findByShowIdOrderByDisplayOrderAsc(Long showId);
    
    void deleteByShowId(Long showId);
}
