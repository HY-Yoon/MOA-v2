package com.moa2.api.user.controller.docs;

import com.moa2.api.auth.dto.AuthDto;
import com.moa2.api.user.dto.UserDto;
import com.moa2.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "마이페이지 - 회원 관리", description = "내 정보 조회 및 회원 탈퇴 API")
public interface UserControllerDocs {
    @Operation(summary = "내 정보 조회", description = """
            로그인한 사용자의 정보를 조회합니다.
            
            **인증 방식:** Cookie (accessToken)
            
            **응답 정보:**
            - 이메일, 이름, 프로필 이미지
            - 소셜 제공자 정보 (Google, Naver, Kakao)
            - 권한 정보 (USER, ADMIN)
            """)
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "성공 응답", value = """
                    {
                      "success": true,
                      "data": {
                        "email": "todayda1006@gmail.com",
                        "name": "오늘다",
                        "picture": "https://lh3.googleusercontent.com/...",
                        "provider": "GOOGLE",
                        "providerId": "123456789",
                        "role": "USER"
                      },
                      "message": null
                    }
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 - 로그인 필요", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "인증 실패", value = """
                    {
                      "success": false,
                      "data": null,
                      "message": "인증이 필요합니다."
                    }
                    """)))
    })
    ResponseEntity<ApiResponse<AuthDto.UserInfoResponse>> getMyInfo();

    @Operation(summary = "회원 탈퇴", description = """
            로그인한 사용자의 계정을 탈퇴합니다.
            
            **탈퇴 조건:**
            - 진행중인 예매(PENDING, CONFIRMED)가 없어야 함
            
            **처리 과정:**
            1. 진행중인 예매 확인
            2. RefreshToken 무효화
            3. 회원 상태를 DELETED로 변경 (Soft Delete)
            
            **주의:** 탈퇴 후에는 복구할 수 없습니다.
            """)
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "탈퇴 성공", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "성공 응답", value = """
                    {
                      "success": true,
                      "data": {
                        "email": "todayda1006@gmail.com",
                        "message": "회원 탈퇴가 완료되었습니다."
                      },
                      "message": "회원 탈퇴가 완료되었습니다."
                    }
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "탈퇴 실패 - 진행중인 예매가 있음", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "진행중인 예매 있음", value = """
                    {
                      "success": false,
                      "data": null,
                      "message": "진행중인 예매가 있어 탈퇴할 수 없습니다. 예매를 취소한 후 다시 시도해주세요."
                    }
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 - 로그인 필요")
    })
    ResponseEntity<ApiResponse<UserDto.UserDeleteResponse>> deleteMyAccount();
}
