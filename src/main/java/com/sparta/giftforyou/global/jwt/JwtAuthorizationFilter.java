package com.sparta.giftforyou.global.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.giftforyou.domain.user.dto.MsgResponseDto;
import com.sparta.giftforyou.global.security.UserDetailsServiceImpl;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class JwtAuthorizationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtAuthorizationFilter(JwtUtil jwtUtil, UserDetailsServiceImpl userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain) throws ServletException, IOException {
        String tokenValue = jwtUtil.getTokenFromRequest(req);
        log.info("[getTokenFromRequest] tokenValue: " + tokenValue);
        if (StringUtils.hasText(tokenValue)) {
            if (!jwtUtil.validateToken(tokenValue)) {
                MsgResponseDto msgResponseDto = new MsgResponseDto(HttpStatus.UNAUTHORIZED.value(), "유효하지 않은 JWT입니다");
                res.setCharacterEncoding("UTF-8");
                res.setContentType("application/json");
                String jsonResponse = new ObjectMapper().writeValueAsString(msgResponseDto);
                res.getWriter().write(jsonResponse);
                return;
            }
            Claims info = jwtUtil.getUserInfoFromToken(tokenValue);

            try {
                setAuthentication(info.getSubject());
            } catch (Exception e) {
                log.error(e.getMessage());
                return;
            }
        }
        filterChain.doFilter(req, res);
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