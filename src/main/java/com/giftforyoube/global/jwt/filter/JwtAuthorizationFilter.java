package com.giftforyoube.global.jwt.filter;

import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.global.jwt.dto.JwtTokenInfo;
import com.giftforyoube.global.jwt.util.FilterResponseUtil;
import com.giftforyoube.global.jwt.util.JwtTokenUtil;
import com.giftforyoube.global.security.UserDetailsServiceImpl;
import com.giftforyoube.user.entity.User;
import com.giftforyoube.user.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
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

    private final JwtTokenUtil jwtTokenUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final UserService userService;

    // 1. JWT 토큰 기반 필터 체인 진행
    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest,
                                    HttpServletResponse httpServletResponse,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = jwtTokenUtil.getTokenFromRequest(httpServletRequest);

        // 1-1. 액세스 토큰이 있는 경우
        if (StringUtils.hasText(token)) {
            String tokenSubstring = jwtTokenUtil.substringToken(token);

            // 2-1. 액세스 토큰이 유효한 경우
            try {
                if (jwtTokenUtil.validateToken(tokenSubstring)) {
                    Claims claims = jwtTokenUtil.getTokenClaims(tokenSubstring);
                    String email = (String) claims.get("email");

                    setAuthentication(email);
                    filterChain.doFilter(httpServletRequest, httpServletResponse);
                    return;
                }

            // 2-2. 액세스 토큰이 만료된 경우
            } catch (Exception e) {

                // 3-1. 리프레시 토큰이 유효한 경우
                log.info("[doFilterInternal] 액세스 토큰 재발급 시도");

                User user = userService.findUserByAccessToken(tokenSubstring);
                String refreshToken = user.getRefreshToken();
                String email = user.getEmail();

                if (userService.isRefreshTokenValid(refreshToken)) {
                    JwtTokenInfo.AccessTokenInfo newAccessTokenInfo = jwtTokenUtil.createAccessTokenInfo(email);
                    Cookie jwtCookie = jwtTokenUtil.addTokenToCookie(newAccessTokenInfo.getAccessToken());
                    httpServletResponse.addCookie(jwtCookie);

                    userService.updateAccessToken(user, newAccessTokenInfo);
                    log.info("[doFilterInternal] 액세스 토큰 재발급 완료");

                    setAuthentication(email);
                    filterChain.doFilter(httpServletRequest, httpServletResponse);
                    return;

                // 3-2. 리프레시 토큰이 만료된 경우
                } else {
                    log.info("[doFilterInternal] 리프레시 토큰 만료");

                    Cookie removedTokenCookie = jwtTokenUtil.removeTokenCookie();
                    httpServletResponse.addCookie(removedTokenCookie);

                    FilterResponseUtil.sendFilterResponse(httpServletResponse,
                            HttpServletResponse.SC_UNAUTHORIZED,
                            BaseResponseStatus.REFRESH_TOKEN_EXPIRED);
                }
            }
        // 1-2. 액세스 토큰이 없는 경우
        } else {
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        }
    }

    // 2. 이메일 기반 유저 인증 & 보안 컨택스트 설정
    public void setAuthentication(String email) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication authentication = createAuthentication(email);
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    // 3. 이메일 기반 유저 인증 객체 생성
    private Authentication createAuthentication(String email) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, null);
        return usernamePasswordAuthenticationToken;
    }
}