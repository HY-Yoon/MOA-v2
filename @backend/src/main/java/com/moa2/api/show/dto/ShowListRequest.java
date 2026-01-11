package com.moa2.api.show.dto;

import com.moa2.global.model.Genre;
import com.moa2.global.model.Region;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

/**
 * 사용자용 공연 목록 조회 요청 DTO
 */
@Getter
@Setter
public class ShowListRequest {
    private Genre genre;              // 장르 필터
    private Region region;            // 지역 필터
    private String keyword;           // 제목 검색
    private LocalDate startDate;      // 공연 시작일 필터
    private LocalDate endDate;        // 공연 종료일 필터
    private String orderBy = "createdAt";        // 정렬 기준 (createdAt, startDate, viewCount, title)
    private String orderDirection = "desc";      // 정렬 방향 (asc, desc)
    private int page = 0;             // 페이지 번호
    private int size = 20;            // 페이지 크기
}
