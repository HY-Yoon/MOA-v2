package com.moa2.api.user.service;

import com.moa2.api.user.domain.entity.User;
import com.moa2.global.model.Gender;
import com.moa2.global.model.SocialProvider;
import com.moa2.global.model.UserStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class UserSpecification {

    public static Specification<User> searchUsers(
            String keyword,
            String searchType, // NAME, EMAIL, PHONE (미입력 시 전체 검색)
            UserStatus status,
            Gender gender,
            SocialProvider socialProvider) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Keyword Search (대소문자 구분 없이)
            if (keyword != null && !keyword.trim().isEmpty()) {
                String searchKeyword = keyword.trim().toLowerCase(); // 소문자 변환

                // searchType이 null이거나 empty면 전체 검색
                if (searchType == null || searchType.trim().isEmpty()) {
                    // 전체 검색: NAME, EMAIL, PHONE 검색
                    Predicate nameLike = criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("name")), "%" + searchKeyword + "%");
                    Predicate emailLike = criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("email")), "%" + searchKeyword + "%");
                    Predicate phoneLike = criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("phone")), "%" + searchKeyword + "%");

                    predicates.add(criteriaBuilder.or(nameLike, emailLike, phoneLike));
                } else {
                    // 특정 타입 검색
                    switch (searchType.toUpperCase()) {
                        case "NAME":
                            predicates.add(criteriaBuilder.like(
                                    criteriaBuilder.lower(root.get("name")), "%" + searchKeyword + "%"));
                            break;
                        case "EMAIL":
                            predicates.add(criteriaBuilder.like(
                                    criteriaBuilder.lower(root.get("email")), "%" + searchKeyword + "%"));
                            break;
                        case "PHONE":
                            predicates.add(criteriaBuilder.like(
                                    criteriaBuilder.lower(root.get("phone")), "%" + searchKeyword + "%"));
                            break;
                        default:
                            // 잘못된 searchType은 무시하고 전체 검색
                            Predicate nameLike = criteriaBuilder.like(
                                    criteriaBuilder.lower(root.get("name")), "%" + searchKeyword + "%");
                            Predicate emailLike = criteriaBuilder.like(
                                    criteriaBuilder.lower(root.get("email")), "%" + searchKeyword + "%");
                            Predicate phoneLike = criteriaBuilder.like(
                                    criteriaBuilder.lower(root.get("phone")), "%" + searchKeyword + "%");
                            predicates.add(criteriaBuilder.or(nameLike, emailLike, phoneLike));
                            break;
                    }
                }
            }

            // 2. Filters
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (gender != null) {
                predicates.add(criteriaBuilder.equal(root.get("gender"), gender));
            }
            if (socialProvider != null) {
                predicates.add(criteriaBuilder.equal(root.get("socialProvider"), socialProvider));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
