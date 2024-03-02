package com.giftforyoube.global.jwt.token.controller;

import com.giftforyoube.global.exception.BaseResponse;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.global.jwt.token.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class TokenController {

    private final TokenService tokenService;

    // 리프레시 토큰으로 액세스 토큰 재발급
    @GetMapping("/access-token/issue")
    public ResponseEntity<BaseResponse> issueAccessToken(HttpServletRequest httpServletRequest,
                                                         HttpServletResponse httpServletResponse) throws UnsupportedEncodingException {
        tokenService.issueAccessToken(httpServletRequest, httpServletResponse);

        return ResponseEntity.status(HttpStatus.OK)
                .body(new BaseResponse<>(BaseResponseStatus.ACCESS_TOKEN_ISSUED));
    }
}