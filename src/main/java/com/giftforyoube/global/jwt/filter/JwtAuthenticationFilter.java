package com.giftforyoube.global.jwt.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.global.jwt.dto.JwtTokenDto;
import com.giftforyoube.global.jwt.token.service.TokenManager;
import com.giftforyoube.global.jwt.util.FilterResponseUtil;
import com.giftforyoube.global.security.UserDetailsImpl;
import com.giftforyoube.user.dto.LoginRequestDto;
import com.giftforyoube.user.entity.User;
import com.giftforyoube.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
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

    private final TokenManager tokenManager;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(TokenManager tokenManager, UserRepository userRepository) {
        this.tokenManager = tokenManager;
        this.userRepository = userRepository;
        setFilterProcessesUrl("/api/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response) throws AuthenticationException {
        log.info("[attemptAuthentication] 로그인 시도");

        try {
            LoginRequestDto loginRequestDto = new ObjectMapper().readValue(request.getInputStream(), LoginRequestDto.class);
            return getAuthenticationManager().authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequestDto.getEmail(),
                            loginRequestDto.getPassword(),
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

        User user = ((UserDetailsImpl) authResult.getPrincipal()).getUser();
        String email = ((UserDetailsImpl) authResult.getPrincipal()).getUsername();
        JwtTokenDto jwtTokenDto = tokenManager.createJwtTokenDto(email);

        Cookie jwtCookie = tokenManager.addTokenToCookie(jwtTokenDto.getAccessToken());
        httpServletResponse.addCookie(jwtCookie);

        user.updateRefreshToken(jwtTokenDto);
        userRepository.save(user);

        FilterResponseUtil.sendFilterResponse(httpServletResponse,
                HttpServletResponse.SC_OK,
                BaseResponseStatus.LOGIN_SUCCESS);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest httpServletRequest,
                                              HttpServletResponse httpServletResponse,
                                              AuthenticationException failed) throws IOException {
        log.info("[unsuccessfulAuthentication] 로그인 실패");

        FilterResponseUtil.sendFilterResponse(httpServletResponse,
                HttpServletResponse.SC_UNAUTHORIZED,
                BaseResponseStatus.LOGIN_FAILED);
    }
}