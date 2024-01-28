package com.sparta.giftforyou.global.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.giftforyou.domain.user.dto.LoginRequestDto;
import com.sparta.giftforyou.domain.user.dto.LoginSuccessResponseDto;
import com.sparta.giftforyou.global.security.UserDetailsImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Slf4j
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
        setFilterProcessesUrl("/api/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        log.info("[login]: 로그인 시도");
        try {
            LoginRequestDto requestDto = new ObjectMapper().readValue(request.getInputStream(), LoginRequestDto.class);

            return getAuthenticationManager().authenticate(
                    new UsernamePasswordAuthenticationToken(
                            requestDto.getEmail(),
                            requestDto.getPassword(),
                            null
                    )
            );
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        log.info("[login]: 로그인 완료");
        String email = ((UserDetailsImpl) authResult.getPrincipal()).getUsername();

        log.info("[login]: JWT 생성");
        String token = jwtUtil.createToken(email);
        String valueToken = jwtUtil.addJwtToCookie(token, response);

        LoginSuccessResponseDto loginSuccessResponseDto = new LoginSuccessResponseDto(valueToken);
        response.setContentType("application/json");
        response.getWriter().write(new ObjectMapper().writeValueAsString(loginSuccessResponseDto));
        response.setStatus(200);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        log.info("[login]: 로그인 실패", failed.getMessage());
        response.setStatus(401);
    }
}