package com.moa2.api.show.domain.repository;

import com.moa2.api.show.domain.entity.DetailImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetailImageRepository extends JpaRepository<DetailImage, Long> {
    
    List<DetailImage> findByShowIdOrderByDisplayOrderAsc(Long showId);
    
    void deleteByShowId(Long showId);
}
