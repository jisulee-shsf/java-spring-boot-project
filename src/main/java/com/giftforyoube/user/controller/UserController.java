package com.giftforyoube.user.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.giftforyoube.global.exception.BaseResponse;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.global.jwt.JwtUtil;
import com.giftforyoube.global.security.UserDetailsImpl;
import com.giftforyoube.user.dto.DeleteRequestDto;
import com.giftforyoube.user.dto.SignupRequestDto;
import com.giftforyoube.user.service.GoogleService;
import com.giftforyoube.user.service.KakaoService;
import com.giftforyoube.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

@Slf4j
@RestController
@RequestMapping("/api")
@Tag(name = "유저", description = "유저 관련 API")
public class UserController {

    private final UserService userService;
    private final KakaoService kakaoService;
    private final GoogleService googleService;

    public UserController(UserService userService, KakaoService kakaoService, GoogleService googleService) {
        this.userService = userService;
        this.kakaoService = kakaoService;
        this.googleService = googleService;
    }

    // 1. 일반 회원가입
    @PostMapping("/signup")
    public ResponseEntity<BaseResponse<Void>> registerAccount(@Valid @RequestBody SignupRequestDto requestDto,
                                                              BindingResult bindingResult) throws MethodArgumentNotValidException {
        userService.registerAccount(requestDto, bindingResult);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new BaseResponse<>(BaseResponseStatus.REGISTER_ACCOUNT_SUCCESS));
    }

    // 2. 일반 회원탈퇴
    @DeleteMapping("/delete")
    public ResponseEntity<BaseResponse<Void>> deleteAccount(@Valid @RequestBody DeleteRequestDto deleteRequestDto,
                                                            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        userService.deleteAccount(userDetails.getUser().getId(), deleteRequestDto.getPassword());
        return ResponseEntity.status(HttpStatus.OK)
                .body(new BaseResponse<>(BaseResponseStatus.DELETE_ACCOUNT_SUCCESS));
    }

    // 3-1. 카카오 로그인(ResponseCookie)
    @PostMapping("/kakao/login")
    public ResponseEntity<BaseResponse<String>> kakaoLogin(@RequestParam String code,
                                                           HttpServletResponse httpServletResponse) throws JsonProcessingException, UnsupportedEncodingException, URISyntaxException {
        String kakaoToken = kakaoService.kakaoLogin(code);

        ResponseCookie cookie = ResponseCookie.from(JwtUtil.AUTHORIZATION_HEADER)
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("None") // SameSite 설정 추가
                .build();

        httpServletResponse.setHeader("Set-Cookie", cookie.toString());
        BaseResponse<String> baseResponse = new BaseResponse<>(BaseResponseStatus.KAKAO_LOGIN_SUCCESS, kakaoToken);
        return ResponseEntity.status(HttpStatus.OK).body(baseResponse);
    }

    // 4-1. 구글 로그인(ResponseCookie)
    @PostMapping("/google/login")
    public ResponseEntity<BaseResponse<String>> googleLogin(@RequestParam String code,
                                                            HttpServletResponse httpServletResponse) throws JsonProcessingException, UnsupportedEncodingException, URISyntaxException {
        String googleToken = googleService.googleLogin(code);
        ResponseCookie cookie = ResponseCookie.from(JwtUtil.AUTHORIZATION_HEADER)
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("None") // SameSite 설정 추가
                .build();

        httpServletResponse.setHeader("Set-Cookie", cookie.toString());
        BaseResponse<String> baseResponse = new BaseResponse<>(BaseResponseStatus.GOOGLE_LOGIN_SUCCESS, googleToken);
        return ResponseEntity.status(HttpStatus.OK).body(baseResponse);
    }
}