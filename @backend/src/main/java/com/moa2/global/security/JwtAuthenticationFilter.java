package com.moa2.global.security;

import com.moa2.global.util.LogMaskingUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 토큰을 검증하고 SecurityContext에 인증 정보를 설정하는 필터
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = getTokenFromRequest(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateAccessToken(token)) {
            String email = jwtTokenProvider.getEmailFromAccessToken(token);
            String provider = jwtTokenProvider.getProviderFromAccessToken(token);
            
            // 인증 객체 생성 (권한은 기본적으로 USER로 설정)
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    email,
                    provider,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("JWT 인증 성공: {}", LogMaskingUtil.maskEmail(email));
        }

        filterChain.doFilter(request, response);
    }

    /**
     * HTTP 요청에서 JWT 토큰 추출 (Cookie에서만)
     * @param request HTTP 요청
     * @return JWT 토큰 문자열 (없으면 null)
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        // Cookie에서 accessToken 확인
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}

