package com.giftforyoube.global.jwt.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.giftforyoube.global.exception.BaseException;
import com.giftforyoube.global.exception.BaseResponse;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.global.jwt.token.service.TokenManager;
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
public class jwtAuthorizationFilter extends OncePerRequestFilter {

    private final TokenManager tokenManager;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest,
                                    HttpServletResponse httpServletResponse,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = tokenManager.getTokenFromRequest(httpServletRequest);

        if (StringUtils.hasText(token)) {
            try {
                if (!tokenManager.validateToken(token)) {
                    BaseResponse<Void> baseResponse = new BaseResponse<>(BaseResponseStatus.INVALID_TOKEN);
                    httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    httpServletResponse.setContentType("application/json;charset=UTF-8");
                    httpServletResponse.getWriter().write(new ObjectMapper().writeValueAsString(baseResponse));
                    return;
                }
                Claims tokenClaims = tokenManager.getTokenClaims(token);
                setAuthentication((String) tokenClaims.get("email"));
            } catch (BaseException e) {
                BaseResponseStatus responseStatus = e.getStatus();

                int statusCode = responseStatus.getHttpStatus().value();
                BaseResponse<Void> baseResponse = new BaseResponse<>(responseStatus);
                httpServletResponse.setStatus(statusCode);
                httpServletResponse.setContentType("application/json;charset=UTF-8");
                httpServletResponse.getWriter().write(new ObjectMapper().writeValueAsString(baseResponse));
                return;
            }
        }

        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }

    public void setAuthentication(String email) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication authentication = createAuthentication(email);
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    private Authentication createAuthentication(String email) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        return new UsernamePasswordAuthenticationToken(userDetails, null, null);
    }
}