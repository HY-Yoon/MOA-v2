package com.moa2.api.payment.controller;

import com.moa2.api.payment.dto.PaymentDto;
import com.moa2.api.payment.exception.PaymentException;
import com.moa2.api.payment.service.PaymentService;
import com.moa2.api.user.domain.entity.User;
import com.moa2.api.user.domain.repository.UserRepository;
import com.moa2.global.dto.ApiResponse;
import com.moa2.global.model.SocialProvider;
import com.moa2.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

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
        private final com.moa2.api.payment.facade.PaymentFacade paymentFacade;
        private final UserRepository userRepository;

        @Value("${app.payment.frontend-complete-url:http://localhost:5173/payment/complete}")
        private String frontendCompleteUrl;

        @Value("${app.payment.frontend-fail-url:http://localhost:5173/payment/fail}")
        private String frontendFailUrl;

        @Value("${app.payment.test-no-redirect:false}")
        private boolean testNoRedirect;

        /**
         * 예매자 확인 정보 조회
         * 결제 과정 중 예매자 확인 단계에서 사용자 정보를 조회합니다.
         * 
         * 쿠키 기반 인증을 통해 현재 로그인한 사용자의 이름, 이메일, 연락처를 반환합니다.
         * 연락처는 없는 경우 null로 반환될 수 있습니다.
         */
        @Operation(summary = "예매자 확인 정보 조회", description = """
                        결제 과정 중 예매자 확인 단계에서 호출합니다.

                        **반환 정보:**
                        - 이름 (name): 로그인 정보에 등록된 이름
                        - 이메일 (email): 로그인 정보에 등록된 이메일
                        - 연락처 (phone): 로그인 정보에 등록된 연락처 (없을 수 있음)

                        **권한:** 인증된 사용자만 가능 (쿠키 기반 인증)
                        """)
        @GetMapping("/buyer-info")
        public ResponseEntity<ApiResponse<PaymentDto.BuyerInfoResponse>> getBuyerInfo() {
                try {
                        Long userId = getAuthenticatedUserId();
                        User user = userRepository.findById(userId)
                                        .orElseThrow(() -> new PaymentException(
                                                        org.springframework.http.HttpStatus.NOT_FOUND,
                                                        "USER_NOT_FOUND",
                                                        "사용자를 찾을 수 없습니다."));

                        PaymentDto.BuyerInfoResponse response = new PaymentDto.BuyerInfoResponse(
                                        user.getName(),
                                        user.getEmail(),
                                        user.getPhone() // nullable
                        );

                        return ResponseEntity.ok(ApiResponse.success(response));

                } catch (PaymentException e) {
                        log.warn("예매자 정보 조회 실패: code={}, message={}", e.getCode(), e.getMessage());
                        return ResponseEntity.status(e.getStatus())
                                        .body(ApiResponse.error(e.getMessage(), e.getCode(), null));
                }
        }

        /**
         * Step 5-1: 결제 요청
         * 프론트가 토스 위젯을 띄우기 전, 사전 데이터를 생성
         */
        @Operation(summary = "결제 요청", description = """
                        프론트에서 토스 결제 위젯을 띄우기 전에 호출합니다.

                        **처리 내용:**
                        1. 좌석 선점 상태 검증 (LOCKED 상태, 본인 선점 여부)
                        2. Reservation, Payment 생성 (PENDING 상태)
                        3. 토스 위젯에 전달할 정보 반환

                        **권한:** 인증된 사용자만 가능
                        """)
        @PostMapping("/request")
        public ResponseEntity<ApiResponse<PaymentDto.RequestResponse>> requestPayment(
                        @Valid @RequestBody PaymentDto.Request request) {
                try {
                        Long userId = getAuthenticatedUserId();
                        PaymentDto.RequestResponse response = paymentService.requestPayment(request, userId);
                        return ResponseEntity.ok(ApiResponse.success(response));

                } catch (PaymentException e) {
                        log.warn("결제 요청 실패: code={}, message={}", e.getCode(), e.getMessage());
                        return ResponseEntity.status(e.getStatus())
                                        .body(ApiResponse.error(e.getMessage(), e.getCode(), null));
                }
        }

        /**
         * Step 5-2: 결제 승인
         * 토스 인증 성공 후(successUrl) 호출, 3단 검증 및 최종 승인
         */
        @Operation(summary = "결제 승인", description = """
                        토스 결제 인증 성공 후(successUrl 리다이렉트) 호출합니다.

                        **3단 검증:**
                        1. 좌석이 LOCKED 상태이고 본인이 선점했는지
                        2. 선점 시간(5분)이 만료되지 않았는지
                        3. 결제 금액이 일치하는지

                        **검증 실패 시:** 토스 결제 취소 API 호출 후 예외 발생
                        **검증 성공 시:** 토스 승인 API 호출, 상태 확정

                        **권한:** 인증된 사용자(본인 예매만)
                        """)
        @PostMapping("/confirm")
        public ResponseEntity<ApiResponse<PaymentDto.SuccessResponse>> confirmPayment(
                        @Valid @RequestBody PaymentDto.ConfirmRequest request) {
                try {
                        Long userId = getAuthenticatedUserId();
                        // Facade 패턴 적용
                        PaymentDto.SuccessResponse response = paymentFacade.confirmPayment(request, userId);
                        return ResponseEntity.ok(ApiResponse.success(response));

                } catch (PaymentException e) {
                        log.warn("결제 승인 실패: orderId={}, code={}, message={}",
                                        request.orderId(), e.getCode(), e.getMessage());
                        return ResponseEntity.status(e.getStatus())
                                        .body(ApiResponse.error(e.getMessage(), e.getCode(), null));
                }
        }

        /**
         * Step 5-3: 결제 실패
         * 토스 인증 중 실패(failUrl) 또는 취소했을 때 호출
         */
        @Operation(summary = "결제 실패 처리", description = """
                        토스 결제 인증 실패 또는 취소 시(failUrl 리다이렉트) 호출합니다.

                        **처리 내용:**
                        1. Payment → FAILED 상태로 변경, 실패 사유 저장
                        2. Reservation → CANCELLED 상태로 변경
                        3. 선점한 좌석(ScheduleSeat) 락 해제 → AVAILABLE

                        **참고:** orderId가 없을 수 있음 (PAY_PROCESS_CANCELED 케이스)
                        """)
        @PostMapping("/fail")
        public ResponseEntity<ApiResponse<Void>> failPayment(
                        @RequestBody PaymentDto.FailRequest request) {
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

        // ------------------------- 토스 success/fail 핸들러 (GET 리다이렉트)
        // -------------------------

        /**
         * 토스 successUrl 핸들러 (GET)
         * 토스 결제 인증 성공 후 리다이렉트 → confirm 처리 → 프론트 완료 페이지로 302
         */
        @Operation(summary = "결제 성공 핸들러 (토스 리다이렉트)", description = """
                        토스가 successUrl로 GET 리다이렉트할 때 호출됩니다.
                        orderId, paymentKey, amount로 결제 승인 후 프론트 완료 페이지로 302 리다이렉트합니다.
                        인증(쿠키) 필요. 미인증 시 완료 페이지 대신 실패 페이지로 리다이렉트합니다.
                        백엔드만 테스트: noRedirect=1 및 app.payment.test-no-redirect=true 이면
                        Mock 승인 후 200 + CompletionResponse JSON 반환 (302 없음).
                        """)
        @GetMapping("/success")
        public ResponseEntity<?> successHandler(
                        @RequestParam(required = false) String orderId,
                        @RequestParam(required = false) String paymentKey,
                        @RequestParam(required = false) Long amount,
                        @RequestParam(name = "noRedirect", required = false) String noRedirect) {
                Long userId;
                try {
                        userId = getAuthenticatedUserId();
                } catch (PaymentException e) {
                        log.warn("결제 성공 핸들러 인증 실패: {}", e.getMessage());
                        if (testNoRedirect && "1".equals(noRedirect)) {
                                return ResponseEntity.status(e.getStatus())
                                                .body(ApiResponse.error(e.getMessage(), e.getCode(), null));
                        }
                        String failUrl = UriComponentsBuilder.fromUriString(frontendFailUrl)
                                        .queryParam("code", "UNAUTHORIZED")
                                        .queryParam("message", e.getMessage())
                                        .build().toUriString();
                        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(failUrl)).build();
                }
                if (orderId == null || orderId.isBlank() || paymentKey == null || paymentKey.isBlank()
                                || amount == null) {
                        log.warn("결제 성공 핸들러 파라미터 누락: orderId={}, paymentKey={}, amount={}", orderId, paymentKey,
                                        amount);
                        if (testNoRedirect && "1".equals(noRedirect)) {
                                return ResponseEntity.badRequest()
                                                .body(ApiResponse.error("결제 정보가 올바르지 않습니다.", "INVALID_PARAMS", null));
                        }
                        String failUrl = UriComponentsBuilder.fromUriString(frontendFailUrl)
                                        .queryParam("code", "INVALID_PARAMS")
                                        .queryParam("message", "결제 정보가 올바르지 않습니다.")
                                        .build().toUriString();
                        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(failUrl)).build();
                }

                boolean useNoRedirect = testNoRedirect && "1".equals(noRedirect);

                if (useNoRedirect) {
                        try {
                                paymentFacade.confirmPaymentMock(
                                                paymentKey != null ? paymentKey : "test_no_redirect_key",
                                                orderId, amount);
                                String rn = paymentService.getReservationNumberByOrderId(orderId);
                                PaymentDto.CompletionResponse completion = paymentService.getCompletionInfo(rn, userId);
                                return ResponseEntity.ok(ApiResponse.success(completion));
                        } catch (PaymentException e) {
                                log.warn("결제 Mock 승인 실패(noRedirect): orderId={}, message={}", orderId, e.getMessage());
                                return ResponseEntity.status(e.getStatus())
                                                .body(ApiResponse.error(e.getMessage(), e.getCode(), null));
                        }
                }

                try {
                        PaymentDto.SuccessResponse resp = paymentFacade.confirmPayment(
                                        new PaymentDto.ConfirmRequest(paymentKey, orderId, amount), userId);
                        String redirectUrl = UriComponentsBuilder.fromUriString(frontendCompleteUrl)
                                        .queryParam("reservationNumber", resp.reservationNumber())
                                        .build().toUriString();
                        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
                } catch (PaymentException e) {
                        log.warn("결제 승인 실패(핸들러): orderId={}, code={}, message={}",
                                        orderId, e.getCode(), e.getMessage());
                        String failUrl = UriComponentsBuilder.fromUriString(frontendFailUrl)
                                        .queryParam("code", e.getCode() != null ? e.getCode() : "CONFIRM_FAILED")
                                        .queryParam("message", e.getMessage())
                                        .queryParam("orderId", orderId)
                                        .build().toUriString();
                        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(failUrl)).build();
                }
        }

        /**
         * 토스 failUrl 핸들러 (GET)
         * 토스 결제 인증 실패/취소 시 리다이렉트 → fail 처리 → 프론트 실패 페이지로 302
         */
        @Operation(summary = "결제 실패 핸들러 (토스 리다이렉트)", description = """
                        토스가 failUrl로 GET 리다이렉트할 때 호출됩니다.
                        orderId, code, message로 실패 처리 후 프론트 실패 페이지로 302 리다이렉트합니다.
                        orderId는 없을 수 있습니다 (PAY_PROCESS_CANCELED 등).
                        백엔드만 테스트: noRedirect=1 및 app.payment.test-no-redirect=true 이면
                        302 대신 200 + TestNoRedirectFailResponse JSON 반환.
                        """)
        @GetMapping("/fail")
        public ResponseEntity<?> failHandler(
                        @RequestParam(required = false) String orderId,
                        @RequestParam(required = false) String code,
                        @RequestParam(required = false) String message,
                        @RequestParam(name = "noRedirect", required = false) String noRedirect) {
                String codeVal = code != null ? code : "UNKNOWN";
                String msgVal = message != null ? message : "결제가 실패하거나 취소되었습니다.";
                try {
                        paymentService.failPayment(new PaymentDto.FailRequest(
                                        orderId != null ? orderId : "",
                                        codeVal,
                                        msgVal));
                } catch (Exception e) {
                        log.error("결제 실패 핸들러 처리 중 오류: orderId={}, error={}", orderId, e.getMessage());
                }
                var b = UriComponentsBuilder.fromUriString(frontendFailUrl)
                                .queryParam("code", codeVal)
                                .queryParam("message", msgVal);
                if (orderId != null && !orderId.isBlank()) {
                        b.queryParam("orderId", orderId);
                }
                String redirectUrl = b.build().toUriString();

                if (testNoRedirect && "1".equals(noRedirect)) {
                        return ResponseEntity.ok(ApiResponse.success(
                                        new PaymentDto.TestNoRedirectFailResponse(codeVal, msgVal, orderId,
                                                        redirectUrl)));
                }
                return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
        }

        /**
         * 결제 완료 페이지용 정보 조회 (예매 완료 정보 노출)
         * GET ?reservationNumber=xxx 또는 ?reservationId=xxx → 공연 기본 정보, 예매자 정보, 결제 정보
         * 반환.
         * reservationNumber(예매번호) / reservationId 둘 중 하나 필수. Mock 응답의 reservationId로도
         * 조회 가능.
         */
        @Operation(summary = "결제 완료 정보 조회", description = """
                        예매 완료 페이지에서 호출합니다.
                        reservationNumber(예매번호) 또는 reservationId로 본인 예매인지 검증 후, 공연/예매자/결제 정보를 반환합니다.
                        Mock 결제 승인 응답의 reservationId로 조회 가능.
                        **권한:** 인증된 사용자 (본인 예매만)
                        """)
        @GetMapping("/complete")
        public ResponseEntity<ApiResponse<PaymentDto.CompletionResponse>> getComplete(
                        @RequestParam(required = false) String reservationNumber,
                        @RequestParam(required = false) Long reservationId) {
                boolean hasNumber = reservationNumber != null && !reservationNumber.isBlank();
                boolean hasId = reservationId != null;
                if (hasNumber == hasId) {
                        return ResponseEntity.badRequest()
                                        .body(ApiResponse.error(
                                                        "reservationNumber 또는 reservationId 중 하나만 필수입니다.",
                                                        "INVALID_PARAMS", null));
                }
                try {
                        Long userId = getAuthenticatedUserId();
                        PaymentDto.CompletionResponse response = hasId
                                        ? paymentService.getCompletionInfoByReservationId(reservationId, userId)
                                        : paymentService.getCompletionInfo(reservationNumber, userId);
                        return ResponseEntity.ok(ApiResponse.success(response));
                } catch (PaymentException e) {
                        log.warn("결제 완료 정보 조회 실패: reservationNumber={}, reservationId={}, code={}, message={}",
                                        reservationNumber, reservationId, e.getCode(), e.getMessage());
                        return ResponseEntity.status(e.getStatus())
                                        .body(ApiResponse.error(e.getMessage(), e.getCode(), null));
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
                                        "인증이 필요합니다.");
                }

                Object principalObj = authentication.getPrincipal();
                if (!(principalObj instanceof UserPrincipal userPrincipal)) {
                        throw new PaymentException(
                                        org.springframework.http.HttpStatus.UNAUTHORIZED,
                                        "INVALID_AUTH",
                                        "인증 정보가 올바르지 않습니다. 다시 로그인해주세요.");
                }

                String email = userPrincipal.getEmail();
                String provider = userPrincipal.getProvider();
                if (provider == null || provider.isBlank()) {
                        throw new PaymentException(
                                        org.springframework.http.HttpStatus.UNAUTHORIZED,
                                        "INVALID_AUTH",
                                        "인증 정보(provider)가 없습니다. 다시 로그인해주세요.");
                }

                SocialProvider socialProvider = SocialProvider.valueOf(provider);
                User user = userRepository.findByEmailAndSocialProvider(email, socialProvider)
                                .orElseThrow(() -> new PaymentException(
                                                org.springframework.http.HttpStatus.NOT_FOUND,
                                                "USER_NOT_FOUND",
                                                "존재하지 않는 사용자입니다."));

                return user.getId();
        }
}
