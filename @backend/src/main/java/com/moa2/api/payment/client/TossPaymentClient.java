package com.moa2.api.payment.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moa2.api.payment.exception.TossPaymentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 토스페이먼츠 API 클라이언트
 * - 결제 승인: POST /v1/payments/confirm
 * - 결제 취소: POST /v1/payments/{paymentKey}/cancel
 */
@Slf4j
@Component
public class TossPaymentClient {

    private static final String TOSS_API_BASE_URL = "https://api.tosspayments.com/v1/payments";

    private final RestTemplate restTemplate;
    private final String secretKey;
    private final ObjectMapper objectMapper;

    public TossPaymentClient(
            RestTemplate restTemplate,
            @Value("${toss.secret-key}") String secretKey,
            ObjectMapper objectMapper
    ) {
        this.restTemplate = restTemplate;
        this.secretKey = secretKey;
        this.objectMapper = objectMapper;
    }

    /**
     * 결제 승인 API 호출
     * POST https://api.tosspayments.com/v1/payments/confirm
     */
    public TossPaymentResponse confirmPayment(String paymentKey, String orderId, Integer amount) {
        String url = TOSS_API_BASE_URL + "/confirm";
        TossConfirmRequest request = new TossConfirmRequest(paymentKey, orderId, amount);

        try {
            ResponseEntity<TossPaymentResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(request, createHeaders()),
                    TossPaymentResponse.class
            );

            log.info("토스 결제 승인 성공: orderId={}, paymentKey={}", orderId, paymentKey);
            return response.getBody();

        } catch (HttpClientErrorException e) {
            log.error("토스 결제 승인 실패: orderId={}, status={}, body={}",
                    orderId, e.getStatusCode(), e.getResponseBodyAsString());
            throw handleTossError(e, "결제 승인");
        }
    }

    /**
     * 결제 취소 API 호출
     * POST https://api.tosspayments.com/v1/payments/{paymentKey}/cancel
     */
    public TossPaymentResponse cancelPayment(String paymentKey, String cancelReason) {
        String url = TOSS_API_BASE_URL + "/" + paymentKey + "/cancel";
        TossCancelRequest request = new TossCancelRequest(cancelReason);

        try {
            ResponseEntity<TossPaymentResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(request, createHeaders()),
                    TossPaymentResponse.class
            );

            log.info("토스 결제 취소 성공: paymentKey={}, reason={}", paymentKey, cancelReason);
            return response.getBody();

        } catch (HttpClientErrorException e) {
            log.error("토스 결제 취소 실패: paymentKey={}, status={}, body={}",
                    paymentKey, e.getStatusCode(), e.getResponseBodyAsString());
            throw handleTossError(e, "결제 취소");
        }
    }

    /**
     * 인증 헤더 생성
     * Authorization: Basic {Base64(secretKey + ":")}
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 시크릿 키 뒤에 ":"을 붙여서 Base64 인코딩
        String credentials = secretKey + ":";
        String encodedCredentials = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        headers.set("Authorization", "Basic " + encodedCredentials);
        return headers;
    }

    /**
     * 토스 API 에러 처리
     */
    private TossPaymentException handleTossError(HttpClientErrorException e, String operation) {
        try {
            String responseBody = e.getResponseBodyAsString();
            if (responseBody != null && !responseBody.isEmpty()) {
                TossErrorResponse errorResponse = objectMapper.readValue(responseBody, TossErrorResponse.class);
                if (errorResponse != null) {
                    return new TossPaymentException(errorResponse.code(), errorResponse.message());
                }
            }
        } catch (Exception parseException) {
            log.warn("토스 에러 응답 파싱 실패", parseException);
        }
        return new TossPaymentException("UNKNOWN_ERROR", operation + " 중 오류가 발생했습니다.");
    }
}
