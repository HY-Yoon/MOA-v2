package com.moa2.global.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 간단한 페이지네이션 응답 DTO
 * content와 필수 페이지 정보만 포함
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> content;
    private int page;           // 현재 페이지 번호 (0부터 시작)
    private int size;           // 페이지 크기
    private long totalElements; // 전체 요소 개수
    private int totalPages;     // 전체 페이지 수
    private boolean first;      // 첫 번째 페이지 여부
    private boolean last;       // 마지막 페이지 여부

    /**
     * Spring Data Page 객체를 PageResponse로 변환
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        return PageResponse.<T>builder()
            .content(page.getContent())
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .first(page.isFirst())
            .last(page.isLast())
            .build();
    }
}

