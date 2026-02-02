package com.moa2.api.user.controller.docs;

import com.moa2.api.user.dto.UserDto;
import com.moa2.global.dto.ApiResponse;
import com.moa2.global.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.moa2.global.model.Gender;
import com.moa2.global.model.SocialProvider;
import com.moa2.global.model.UserStatus;
import org.springframework.http.ResponseEntity;

@Tag(name = "회원 관리 API", description = "관리자용 회원 관리 API")
public interface AdminUserControllerDocs {
  @Operation(summary = "회원 목록 조회", description = "관리자가 회원 목록을 조회합니다. (검색, 정렬, 페이징 포함)")
  ResponseEntity<ApiResponse<PageResponse<UserDto.UserListResponse>>> getUserList(
      @Parameter(description = "검색 키워드 (대소문자 구분 없이)", example = "홍길동") String keyword,

      @Parameter(description = "정렬 기준 (field,direction). 예시: email,desc | email,asc | createdAt,desc | createdAt,asc", example = "createdAt,desc") String sort,

      @Parameter(description = "검색 타입 (NAME, EMAIL, PHONE). 미입력 시 전체 검색", example = "NAME") String searchType,

      @Parameter(description = "회원 상태 (ACTIVE, SUSPENDED, DELETED)", example = "ACTIVE") UserStatus status,

      @Parameter(description = "성별 (MALE, FEMALE, OTHER)", example = "MALE") Gender gender,

      @Parameter(description = "소셜 제공자 (KAKAO, NAVER, GOOGLE)", example = "KAKAO") SocialProvider socialProvider,

      @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") int page,

      @Parameter(description = "페이지 크기", example = "20") int size);

  @Operation(summary = "회원 상태 변경", description = "관리자가 회원 상태(ACTIVE/SUSPENDED)를 변경합니다.")
  ResponseEntity<ApiResponse<UserDto.UserStatusResponse>> changeUserStatus(
      @Parameter(description = "회원 ID", example = "1", required = true) Long userId,

      // ▼ [중요] 여기가 기존 코드의 Swagger 설정을 그대로 가져오는 부분입니다.
      // Spring의 RequestBody가 아니라 Swagger의 어노테이션을 사용합니다.
      @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "상태 변경 요청 정보", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.UserStatusChangeRequest.class), examples = @ExampleObject(name = "계정 정지 예시", value = """
          {
            "status": "SUSPENDED",
            "reason": "운영 정책 위반"
          }
          """))) UserDto.UserStatusChangeRequest request);
}
