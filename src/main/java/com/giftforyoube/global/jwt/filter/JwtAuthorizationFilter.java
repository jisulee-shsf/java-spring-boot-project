package com.giftforyoube.global.jwt.filter;

import com.giftforyoube.global.exception.BaseException;
import com.giftforyoube.global.jwt.token.service.TokenManager;
import com.giftforyoube.global.jwt.util.FilterResponseUtil;
import com.giftforyoube.global.security.UserDetailsServiceImpl;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final TokenManager tokenManager;
    private final UserDetailsServiceImpl userDetailsService;

    // 1. 토큰 기반 필터 체인 진행
    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest,
                                    HttpServletResponse httpServletResponse,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = tokenManager.getTokenFromRequest(httpServletRequest);

        if (StringUtils.hasText(token)) {
            String tokenValue = tokenManager.substringToken(token);
            try {
                if (tokenManager.validateToken(tokenValue)) {
                    Claims claims = tokenManager.getTokenClaims(tokenValue);
                    setAuthentication((String) claims.get("email"));
                    filterChain.doFilter(httpServletRequest, httpServletResponse);
                }
            } catch (BaseException e) {
                FilterResponseUtil.sendFilterResponse(httpServletResponse,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        e.getStatus());
            }
        } else {
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        }
    }

    // 2. 이메일 기반 사용자 인증 & 보안 컨택스트 설정
    public void setAuthentication(String email) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication authentication = createAuthentication(email);
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    // 3. 이메일 기반 사용자 인증 객체 생성
    private Authentication createAuthentication(String email) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        return new UsernamePasswordAuthenticationToken(userDetails, null, null);
    }
}