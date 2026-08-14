package com.doquest.global.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // HTTP 요청 헤더에서 JWT 토큰 추출
        String token = resolveToken(request);

        // 토큰 유효성 검증
        if (StringUtils.hasText(token) && jwtProvider.validateToken(token)) {
            Long memberId = jwtProvider.getMemberId(token);

            // Spring Security 인증 객체(Authentication) 생성
            // (권한 목록은 현재 사용하지 않으므로 emptyList 전달)
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(memberId, null, Collections.emptyList());

            // SecurityContext에 인증 정보 저장 (이후 컨트롤러에서 인증된 유저로 인식)
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더에서 'Bearer ' 접두사 제거 후 순수 토큰값만 추출
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}