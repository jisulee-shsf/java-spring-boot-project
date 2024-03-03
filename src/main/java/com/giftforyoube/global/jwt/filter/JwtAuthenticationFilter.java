package com.giftforyoube.global.jwt.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.global.jwt.dto.JwtTokenInfo;
import com.giftforyoube.global.jwt.util.FilterResponseUtil;
import com.giftforyoube.global.jwt.util.JwtTokenUtil;
import com.giftforyoube.global.security.UserDetailsImpl;
import com.giftforyoube.user.dto.LoginRequestDto;
import com.giftforyoube.user.entity.User;
import com.giftforyoube.user.service.UserService;
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

    private final JwtTokenUtil jwtTokenUtil;
    private final UserService userService;

    public JwtAuthenticationFilter(JwtTokenUtil jwtTokenUtil, UserService userService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.userService = userService;
        setFilterProcessesUrl("/api/login");
    }

    // 1. 일반 로그인 시도
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response) throws AuthenticationException {
        log.info("[attemptAuthentication] 일반 로그인 시도");

        try {
            LoginRequestDto loginRequestDto = new ObjectMapper().readValue(request.getInputStream(), LoginRequestDto.class);
            String email = loginRequestDto.getEmail();
            String password = loginRequestDto.getPassword();
            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                    new UsernamePasswordAuthenticationToken(email, password, null);
            return getAuthenticationManager().authenticate(usernamePasswordAuthenticationToken);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    // 2-1. 일반 로그인 성공
    @Override
    protected void successfulAuthentication(HttpServletRequest httpServletRequest,
                                            HttpServletResponse httpServletResponse,
                                            FilterChain chain,
                                            Authentication authResult) throws IOException {
        log.info("[successfulAuthentication] 일반 로그인 완료");

        User user = ((UserDetailsImpl) authResult.getPrincipal()).getUser();
        String email = user.getEmail();
        JwtTokenInfo.AccessTokenInfo accessTokenInfo = jwtTokenUtil.createAccessTokenInfo(email);
        JwtTokenInfo.RefreshTokenInfo refreshTokenInfo = jwtTokenUtil.createRefreshTokenInfo(email);

        Cookie jwtCookie = jwtTokenUtil.addTokenToCookie(accessTokenInfo.getAccessToken());
        httpServletResponse.addCookie(jwtCookie);

        userService.updateAccessToken(user, accessTokenInfo);
        userService.updateRefreshToken(user, refreshTokenInfo);

        FilterResponseUtil.sendFilterResponse(httpServletResponse,
                HttpServletResponse.SC_OK,
                BaseResponseStatus.LOGIN_SUCCESS);
    }

    // 2-2. 일반 로그인 실패
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest httpServletRequest,
                                              HttpServletResponse httpServletResponse,
                                              AuthenticationException failed) throws IOException {
        log.info("[unsuccessfulAuthentication] 일반 로그인 실패");

        FilterResponseUtil.sendFilterResponse(httpServletResponse,
                HttpServletResponse.SC_UNAUTHORIZED,
                BaseResponseStatus.LOGIN_FAILED);
    }
}