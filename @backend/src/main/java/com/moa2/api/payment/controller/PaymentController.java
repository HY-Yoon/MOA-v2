package com.moa2.api.payment.controller;

import com.moa2.api.payment.dto.PaymentDto;
import com.moa2.api.payment.exception.PaymentException;
import com.moa2.api.payment.service.PaymentService;
import com.moa2.domain.user.entity.User;
import com.moa2.domain.user.repository.UserRepository;
import com.moa2.global.dto.ApiResponse;
import com.moa2.global.model.SocialProvider;
import com.moa2.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 결제 컨트롤러
 * - 결제 요청 (Step 5-1)
 * - 결제 승인 (Step 5-2)
 * - 결제 실패 (Step 5-3)
 */
@Slf4j
@Tag(name = "결제 API", description = "토스페이먼츠 결제 연동 API")
@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;

    /**
     * Step 5-1: 결제 요청
     * 프론트가 토스 위젯을 띄우기 전, 사전 데이터를 생성
     */
    @Operation(
            summary = "결제 요청",
            description = """
                    프론트에서 토스 결제 위젯을 띄우기 전에 호출합니다.
                    
                    **처리 내용:**
                    1. 좌석 선점 상태 검증 (LOCKED 상태, 본인 선점 여부)
                    2. Reservation, Payment 생성 (PENDING 상태)
                    3. 토스 위젯에 전달할 정보 반환
                    
                    **권한:** 인증된 사용자만 가능
                    """
    )
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<PaymentDto.RequestResponse>> requestPayment(
            @Valid @RequestBody PaymentDto.Request request
    ) {
        try {
            Long userId = getAuthenticatedUserId();
            PaymentDto.RequestResponse response = paymentService.requestPayment(request, userId);
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (PaymentException e) {
            log.warn("결제 요청 실패: code={}, message={}", e.getCode(), e.getMessage());
            return ResponseEntity.status(e.getStatus())
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Step 5-2: 결제 승인
     * 토스 인증 성공 후(successUrl) 호출, 3단 검증 및 최종 승인
     */
    @Operation(
            summary = "결제 승인",
            description = """
                    토스 결제 인증 성공 후(successUrl 리다이렉트) 호출합니다.
                    
                    **3단 검증:**
                    1. 좌석이 LOCKED 상태이고 본인이 선점했는지
                    2. 선점 시간(5분)이 만료되지 않았는지
                    3. 결제 금액이 일치하는지
                    
                    **검증 실패 시:** 토스 결제 취소 API 호출 후 예외 발생
                    **검증 성공 시:** 토스 승인 API 호출, 상태 확정
                    
                    **권한:** 인증된 사용자(본인 예매만)
                    """
    )
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<PaymentDto.SuccessResponse>> confirmPayment(
            @Valid @RequestBody PaymentDto.ConfirmRequest request
    ) {
        try {
            Long userId = getAuthenticatedUserId();
            PaymentDto.SuccessResponse response = paymentService.confirmPayment(request, userId);
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (PaymentException e) {
            log.warn("결제 승인 실패: orderId={}, code={}, message={}",
                    request.orderId(), e.getCode(), e.getMessage());
            return ResponseEntity.status(e.getStatus())
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Step 5-3: 결제 실패
     * 토스 인증 중 실패(failUrl) 또는 취소했을 때 호출
     */
    @Operation(
            summary = "결제 실패 처리",
            description = """
                    토스 결제 인증 실패 또는 취소 시(failUrl 리다이렉트) 호출합니다.
                    
                    **처리 내용:**
                    1. Payment → FAILED 상태로 변경, 실패 사유 저장
                    2. Reservation → CANCELLED 상태로 변경
                    3. 선점한 좌석(ScheduleSeat) 락 해제 → AVAILABLE
                    
                    **참고:** orderId가 없을 수 있음 (PAY_PROCESS_CANCELED 케이스)
                    """
    )
    @PostMapping("/fail")
    public ResponseEntity<ApiResponse<Void>> failPayment(
            @RequestBody PaymentDto.FailRequest request
    ) {
        try {
            paymentService.failPayment(request);
            return ResponseEntity.ok(ApiResponse.success(null));

        } catch (Exception e) {
            log.error("결제 실패 처리 중 오류: orderId={}, error={}",
                    request.orderId(), e.getMessage());
            // 실패 처리는 최대한 성공 응답 (클라이언트 UX)
            return ResponseEntity.ok(ApiResponse.success(null));
        }
    }

    /**
     * SecurityContext에서 인증된 사용자의 ID를 가져옴
     */
    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            throw new PaymentException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "UNAUTHORIZED",
                    "인증이 필요합니다."
            );
        }

        Object principalObj = authentication.getPrincipal();
        if (!(principalObj instanceof UserPrincipal userPrincipal)) {
            throw new PaymentException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "INVALID_AUTH",
                    "인증 정보가 올바르지 않습니다. 다시 로그인해주세요."
            );
        }

        String email = userPrincipal.getEmail();
        String provider = userPrincipal.getProvider();
        if (provider == null || provider.isBlank()) {
            throw new PaymentException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "INVALID_AUTH",
                    "인증 정보(provider)가 없습니다. 다시 로그인해주세요."
            );
        }

        SocialProvider socialProvider = SocialProvider.valueOf(provider);
        User user = userRepository.findByEmailAndSocialProvider(email, socialProvider)
                .orElseThrow(() -> new PaymentException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND",
                        "존재하지 않는 사용자입니다."
                ));

        return user.getId();
    }
}
