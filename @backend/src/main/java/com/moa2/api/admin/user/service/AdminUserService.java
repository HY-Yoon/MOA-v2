package com.moa2.api.admin.user.service;

import com.moa2.api.admin.user.dto.UserListResponse;
import com.moa2.api.admin.user.dto.UserStatusChangeRequest;
import com.moa2.api.admin.user.dto.UserStatusResponse;
import com.moa2.domain.user.entity.User;
import com.moa2.domain.user.repository.UserRepository;
import com.moa2.global.model.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {
    
    private final UserRepository userRepository;
    
    /**
     * 회원 목록 조회
     * @param keyword 검색 키워드 (이름/이메일, optional)
     * @param sort 정렬 기준 (예: "createdAt,desc", default: "createdAt,desc")
     * @param pageable 페이징 정보
     * @return 회원 목록
     */
    public Page<UserListResponse> getUserList(String keyword, String sort, Pageable pageable) {
        // sort 파라미터 파싱 (예: "createdAt,desc" -> Sort.by("createdAt").descending())
        Sort sortObj = parseSort(sort);
        
        // Pageable에 Sort 적용
        Pageable sortedPageable = org.springframework.data.domain.PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            sortObj
        );
        
        // keyword가 있으면 검색, 없으면 전체 조회
        Page<User> users;
        if (keyword != null && !keyword.trim().isEmpty()) {
            users = userRepository.searchByKeyword(keyword.trim(), sortedPageable);
        } else {
            users = userRepository.findAll(sortedPageable);
        }
        
        // DTO 변환
        List<UserListResponse> content = users.getContent().stream()
            .map(user -> UserListResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone()) // null 가능
                .gender(user.getGender() != null ? user.getGender().name() : null)
                .socialProvider(user.getSocialProvider())
                .status(user.getStatus())
                .isVerified(user.getIsVerified())
                .createdAt(user.getCreatedAt())
                .build())
            .collect(Collectors.toList());
        
        return new PageImpl<>(content, sortedPageable, users.getTotalElements());
    }
    
    /**
     * sort 파라미터를 Sort 객체로 변환
     * @param sort "field,direction" 형식 (예: "createdAt,desc", "name,asc")
     * @return Sort 객체
     */
    private Sort parseSort(String sort) {
        if (sort == null || sort.trim().isEmpty()) {
            // 기본값: createdAt,desc
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        
        String[] parts = sort.split(",");
        if (parts.length != 2) {
            // 형식이 맞지 않으면 기본값 반환
            log.warn("잘못된 sort 형식: {}. 기본값(createdAt,desc) 사용", sort);
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        
        String field = parts[0].trim();
        String direction = parts[1].trim().toLowerCase();
        
        Sort.Direction sortDirection = direction.equals("asc") 
            ? Sort.Direction.ASC 
            : Sort.Direction.DESC;
        
        return Sort.by(sortDirection, field);
    }
    
    /**
     * 회원 상태 변경
     * @param userId 회원 ID
     * @param request 상태 변경 요청
     * @return 변경된 회원 정보
     */
    @Transactional
    public UserStatusResponse changeUserStatus(Long userId, UserStatusChangeRequest request) {
        // 회원 조회
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다: " + userId));
        
        // 상태 변경 처리
        if (request.getStatus() == UserStatus.SUSPENDED) {
            // 정지 처리
            String reason = request.getReason();
            if (reason == null || reason.trim().isEmpty()) {
                throw new IllegalArgumentException("정지 사유는 필수입니다");
            }
            user.suspend(reason);
        } else if (request.getStatus() == UserStatus.ACTIVE) {
            // 활성화 처리
            user.activate();
        } else {
            // DELETED는 직접 변경 불가 (withdraw 메서드 사용)
            throw new IllegalArgumentException("지원하지 않는 상태입니다: " + request.getStatus());
        }
        
        // 저장 (JPA가 자동으로 업데이트하지만 명시적으로 저장)
        user = userRepository.save(user);
        
        // 응답 DTO 생성
        return UserStatusResponse.builder()
            .id(user.getId())
            .name(user.getName())
            .email(user.getEmail())
            .status(user.getStatus())
            .suspensionReason(user.getSuspensionReason())
            .updatedAt(user.getUpdatedAt())
            .build();
    }
}

