package com.sparta.giftforyou.global.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.giftforyou.domain.user.dto.LoginRequestDto;
import com.sparta.giftforyou.domain.user.dto.MsgResponseDto;
import com.sparta.giftforyou.global.security.UserDetailsImpl;
import com.sparta.giftforyou.global.security.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
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
        log.info("[login] 로그인 완료");
        String email = ((UserDetailsImpl) authResult.getPrincipal()).getUser().getEmail();

        log.info("[login] JWT 생성");
        String token = jwtUtil.createToken(email);
        String valueToken = jwtUtil.addJwtToCookie(token, response);

        MsgResponseDto msgResponseDto = new MsgResponseDto(valueToken, HttpStatus.OK.value(), "로그인이 완료되었습니다."); // HttpStatus 200
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write(new ObjectMapper().writeValueAsString(msgResponseDto));
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        if (failed instanceof UserDetailsServiceImpl.CustomAuthenticationException) {
            UserDetailsServiceImpl.CustomAuthenticationException customException = (UserDetailsServiceImpl.CustomAuthenticationException) failed;
            MsgResponseDto msgResponseDto = customException.getMsgResponseDto();
            respondWithJson(response, msgResponseDto);
        } else if (failed instanceof BadCredentialsException) {
            handleBadCredentials(response);
        } else {
            handleOtherFailures(response, failed);
        }
    }

    private void handleBadCredentials(HttpServletResponse response) throws IOException {
        MsgResponseDto msgResponseDto = new MsgResponseDto(HttpStatus.UNAUTHORIZED.value(), "비밀번호가 틀립니다. 다시 입력해 주세요.");
        respondWithJson(response, msgResponseDto);
    }

    private void handleOtherFailures(HttpServletResponse response, AuthenticationException failed) throws IOException {
        String errorMessage = failed.getMessage();
        MsgResponseDto msgResponseDto = new MsgResponseDto(HttpStatus.UNAUTHORIZED.value(), errorMessage);
        respondWithJson(response, msgResponseDto);
    }

    private void respondWithJson(HttpServletResponse response, MsgResponseDto msgResponseDto) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write(new ObjectMapper().writeValueAsString(msgResponseDto));
    }
}