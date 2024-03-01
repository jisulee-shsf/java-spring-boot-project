package com.giftforyoube.global.jwt.token.service;

import com.giftforyoube.global.jwt.constant.GrantType;
import com.giftforyoube.global.jwt.token.dto.AccessTokenResponseDto;
import com.giftforyoube.global.jwt.util.AuthorizationHeaderUtil;
import com.giftforyoube.user.entity.User;
import com.giftforyoube.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@Transactional
@RequiredArgsConstructor
public class TokenService {

    private final UserService userService;
    private final TokenManager tokenManager;

    public AccessTokenResponseDto createAccessTokenByRefreshToken(HttpServletRequest httpServletRequest) {

        // Authorization 헤더 검증
        String authorizationHeader = httpServletRequest.getHeader("Authorization");
        AuthorizationHeaderUtil.validateAuthorization(authorizationHeader);

        // 해당 리프레시 토큰을 가진 사용자 확인
        String refreshToken = authorizationHeader.split(" ")[1];
        User user = userService.findUserByRefreshToken(refreshToken);

        // 액세스 토큰 생성 후 DTO 반환
        Date accessTokenExpireTime = tokenManager.createAccessTokenExpireTime();
        String accessToken = tokenManager.createAccessToken(user.getEmail(), accessTokenExpireTime);

        return AccessTokenResponseDto.builder()
                .grantType(GrantType.BEARER.getType())
                .accessToken(accessToken)
                .accessTokenExpireTime(accessTokenExpireTime)
                .build();
    }
}