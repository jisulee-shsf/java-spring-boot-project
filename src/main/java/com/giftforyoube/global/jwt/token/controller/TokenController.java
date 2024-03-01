package com.giftforyoube.global.jwt.token.controller;

import com.giftforyoube.global.exception.BaseResponse;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.global.jwt.token.dto.AccessTokenResponseDto;
import com.giftforyoube.global.jwt.token.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class TokenController {

    private final TokenService tokenService;

    // 리프레시 토큰으로 액세스 토큰 재발급
    @PostMapping("/access-token/issue")
    public ResponseEntity<BaseResponse<AccessTokenResponseDto>> createAccessTokenByRefreshToken(HttpServletRequest httpServletRequest) {
        AccessTokenResponseDto accessTokenResponseDto = tokenService.createAccessTokenByRefreshToken(httpServletRequest);

        return ResponseEntity.status(HttpStatus.OK)
                .body(new BaseResponse<>(BaseResponseStatus.ACCESS_TOKEN_ISSUED, accessTokenResponseDto));
    }
}