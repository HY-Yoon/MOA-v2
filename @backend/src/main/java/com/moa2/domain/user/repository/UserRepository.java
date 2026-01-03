package com.moa2.domain.user.repository;

import com.moa2.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 이름 또는 이메일로 검색 (LIKE 검색)
     * phone은 검색 대상에서 제외
     */
    @Query("SELECT u FROM User u WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "u.name LIKE %:keyword% OR u.email LIKE %:keyword%)")
    Page<User> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}

