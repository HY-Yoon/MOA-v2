package com.moa2.api.payment.controller;

import com.moa2.api.payment.controller.docs.PaymentControllerDocs;
import com.moa2.api.payment.dto.PaymentDto;
import com.moa2.api.payment.exception.PaymentException;
import com.moa2.api.payment.facade.PaymentFacade;
import com.moa2.api.payment.service.PaymentService;
import com.moa2.api.user.domain.entity.User;
import com.moa2.api.user.domain.repository.UserRepository;
import com.moa2.global.dto.ApiResponse;
import com.moa2.global.model.SocialProvider;
import com.moa2.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * 결제 컨트롤러
 * - 결제 요청 (Step 5-1)
 * - 결제 승인 (Step 5-2)
 * - 결제 실패 (Step 5-3)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController implements PaymentControllerDocs {

        private final PaymentService paymentService;
        private final PaymentFacade paymentFacade;
        private final UserRepository userRepository;

        @Value("${app.payment.frontend-complete-url:http://localhost:5173/payment/complete}")
        private String frontendCompleteUrl;

        @Value("${app.payment.frontend-fail-url:http://localhost:5173/payment/fail}")
        private String frontendFailUrl;

        @Value("${app.payment.test-no-redirect:false}")
        private boolean testNoRedirect;

        @Override
        @GetMapping("/buyer-info")
        public ResponseEntity<ApiResponse<PaymentDto.BuyerInfoResponse>> getBuyerInfo() {
                try {
                        Long userId = getAuthenticatedUserId();
                        User user = userRepository.findById(userId)
                                        .orElseThrow(() -> new PaymentException(
                                                        HttpStatus.NOT_FOUND,
                                                        "USER_NOT_FOUND",
                                                        "사용자를 찾을 수 없습니다."));

                        return ResponseEntity.ok(ApiResponse.success(PaymentDto.BuyerInfoResponse.from(user)));

                } catch (PaymentException e) {
                        log.warn("예매자 정보 조회 실패: code={}, message={}", e.getCode(), e.getMessage());
                        return ResponseEntity.status(e.getStatus())
                                        .body(ApiResponse.error(e.getMessage(), e.getCode(), null));
                }
        }

        @Override
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

        @Override
        @PostMapping("/confirm")
        public ResponseEntity<ApiResponse<PaymentDto.SuccessResponse>> confirmPayment(
                        @Valid @RequestBody PaymentDto.ConfirmRequest request) {
                try {
                        Long userId = getAuthenticatedUserId();
                        PaymentDto.SuccessResponse response = paymentFacade.confirmPayment(request, userId);
                        return ResponseEntity.ok(ApiResponse.success(response));

                } catch (PaymentException e) {
                        log.warn("결제 승인 실패: orderId={}, code={}, message={}",
                                        request.orderId(), e.getCode(), e.getMessage());
                        return ResponseEntity.status(e.getStatus())
                                        .body(ApiResponse.error(e.getMessage(), e.getCode(), null));
                }
        }

        @Override
        @PostMapping("/fail")
        public ResponseEntity<ApiResponse<Void>> failPayment(@RequestBody PaymentDto.FailRequest request) {
                try {
                        paymentService.failPayment(request);
                        return ResponseEntity.ok(ApiResponse.success(null));

                } catch (Exception e) {
                        log.error("결제 실패 처리 중 오류: orderId={}, error={}", request.orderId(), e.getMessage());
                        return ResponseEntity.ok(ApiResponse.success(null));
                }
        }

        @Override
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
                                                paymentKey,
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

        @Override
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

//        @Override
//        @GetMapping("/complete")
//        public ResponseEntity<ApiResponse<PaymentDto.CompletionResponse>> getComplete(
//                        @RequestParam(required = false) String reservationNumber,
//                        @RequestParam(required = false) Long reservationId) {
//
//                boolean hasNumber = reservationNumber != null && !reservationNumber.isBlank();
//                boolean hasId = reservationId != null;
//                if (hasNumber == hasId) {
//                        return ResponseEntity.badRequest()
//                                        .body(ApiResponse.error("reservationNumber 또는 reservationId 중 하나만 필수입니다."));
//                }
//                try {
//                        Long userId = getAuthenticatedUserId();
//                        PaymentDto.CompletionResponse response = hasId
//                                        ? paymentService.getCompletionInfoByReservationId(reservationId, userId)
//                                        : paymentService.getCompletionInfo(reservationNumber, userId);
//                        return ResponseEntity.ok(ApiResponse.success(response));
//                } catch (PaymentException e) {
//                        log.warn("결제 완료 정보 조회 실패: reservationNumber={}, reservationId={}, code={}, message={}",
//                                        reservationNumber, reservationId, e.getCode(), e.getMessage());
//                        return ResponseEntity.status(e.getStatus())
//                                        .body(ApiResponse.error(e.getMessage(), e.getCode(), null));
//                }
//        }

        /**
         * SecurityContext에서 인증된 사용자의 ID를 가져옴
         */
        private Long getAuthenticatedUserId() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (authentication == null || !authentication.isAuthenticated()
                                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
                        throw new PaymentException(
                                        HttpStatus.UNAUTHORIZED,
                                        "UNAUTHORIZED",
                                        "인증이 필요합니다.");
                }

                Object principalObj = authentication.getPrincipal();
                if (!(principalObj instanceof UserPrincipal userPrincipal)) {
                        throw new PaymentException(
                                        HttpStatus.UNAUTHORIZED,
                                        "INVALID_AUTH",
                                        "인증 정보가 올바르지 않습니다. 다시 로그인해주세요.");
                }

                String email = userPrincipal.getEmail();
                String provider = userPrincipal.getProvider();
                if (provider == null || provider.isBlank()) {
                        throw new PaymentException(
                                        HttpStatus.UNAUTHORIZED,
                                        "INVALID_AUTH",
                                        "인증 정보(provider)가 없습니다. 다시 로그인해주세요.");
                }

                SocialProvider socialProvider = SocialProvider.valueOf(provider);
                User user = userRepository.findByEmailAndSocialProvider(email, socialProvider)
                                .orElseThrow(() -> new PaymentException(
                                                HttpStatus.NOT_FOUND,
                                                "USER_NOT_FOUND",
                                                "존재하지 않는 사용자입니다."));

                return user.getId();
        }
}
