package com.giftforyoube.global.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.giftforyoube.global.exception.BaseResponse;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.global.security.UserDetailsImpl;
import com.giftforyoube.user.dto.LoginRequestDto;
import jakarta.servlet.FilterChain;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
        setFilterProcessesUrl("/api/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response) throws AuthenticationException {
        log.info("[attemptAuthentication] 로그인 시도");
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
    protected void successfulAuthentication(HttpServletRequest httpServletRequest,
                                            HttpServletResponse httpServletResponse,
                                            FilterChain chain,
                                            Authentication authResult) throws IOException {
        log.info("[successfulAuthentication] 로그인 완료");
        String email = ((UserDetailsImpl) authResult.getPrincipal()).getUsername();
        String token = jwtUtil.createToken(email);
        String valueToken = jwtUtil.addJwtToCookie(token, httpServletResponse);

        BaseResponse baseResponse = new BaseResponse(BaseResponseStatus.LOGIN_SUCCESS, valueToken);
        httpServletResponse.setContentType("application/json;charset=UTF-8");
        httpServletResponse.getWriter().write(objectMapper.writeValueAsString(baseResponse));
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest httpServletRequest,
                                              HttpServletResponse httpServletResponse,
                                              AuthenticationException failed) throws IOException {
        log.info("[unsuccessfulAuthentication] 로그인 실패");
        BaseResponse baseResponse = new BaseResponse<>(BaseResponseStatus.LOGIN_FAILURE);
        httpServletResponse.setContentType("application/json;charset=UTF-8");
        httpServletResponse.getWriter().write(objectMapper.writeValueAsString(baseResponse));
        httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
}