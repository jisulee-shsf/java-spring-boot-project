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
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;
    private final KakaoService kakaoService;
    private final GoogleService googleService;
    private final HttpSession session;

    public UserController(UserService userService, KakaoService kakaoService, GoogleService googleService, HttpSession session) {
        this.userService = userService;
        this.kakaoService = kakaoService;
        this.googleService = googleService;
        this.session = session;
    }

    // 1. 일반 회원가입
    @PostMapping("/signup")
    public ResponseEntity<BaseResponse<Void>> registerAccount(@Valid @RequestBody SignupRequestDto requestDto, BindingResult bindingResult) throws MethodArgumentNotValidException {
        return userService.registerAccount(requestDto, bindingResult);
    }

    // 2. 일반 회원탈퇴
    @DeleteMapping("/delete")
    public BaseResponse<Void> deleteAccount(@RequestBody DeleteRequestDto deleteRequestDto, Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userService.deleteAccount(userDetails.getUser().getId(), deleteRequestDto.getPassword());
    }

    // 3-1. Kakao 로그인
    @GetMapping("/kakao/callback")
    public ResponseEntity<BaseResponse<String>> kakaoLogin(@RequestParam String code, HttpServletResponse response) throws JsonProcessingException, URISyntaxException, UnsupportedEncodingException, MalformedURLException {
        String kakaoToken = kakaoService.kakaoLogin(code);
        session.setAttribute("kakaoToken", kakaoToken);
        log.info("[kakaoLogin] kakaoToken: " + kakaoToken);

        Cookie cookie = new Cookie(JwtUtil.AUTHORIZATION_HEADER, kakaoToken);
        cookie.setPath("/");
        response.addCookie(cookie);

        BaseResponse<String> baseResponse = new BaseResponse<>(BaseResponseStatus.KAKAO_LOGIN_SUCCESS, kakaoToken);
        return ResponseEntity.status(HttpStatus.FOUND) // 302
                .location(new URI("https://www.giftipie.me/"))
                .body(baseResponse); // 2000
    }

    // 3-2. Kakao 로그인 테스트용
    @GetMapping("/kakao/response")
    public ResponseEntity<BaseResponse<String>> getKakaoLoginInfo() {
        String kakaoToken = (String) session.getAttribute("kakaoToken");
        BaseResponse<String> baseResponse = new BaseResponse<>(BaseResponseStatus.KAKAO_LOGIN_SUCCESS, kakaoToken); // 2000
        return ResponseEntity.status(HttpStatus.OK) // 200
                .body(baseResponse);
    }

    // 4. Google 로그인
    @GetMapping("/login/oauth2/code/google")
    public ResponseEntity<BaseResponse<String>> googleLogin(@RequestParam String code, HttpServletResponse response) throws JsonProcessingException, URISyntaxException, UnsupportedEncodingException {
        String googleToken = googleService.googleLogin(code);
        log.info("[googleLogin] googleToken: " + googleToken);

        Cookie cookie = new Cookie(JwtUtil.AUTHORIZATION_HEADER, googleToken);
        cookie.setPath("/");
        response.addCookie(cookie);

        BaseResponse<String> baseResponse = new BaseResponse<>(BaseResponseStatus.GOOGLE_LOGIN_SUCCESS, googleToken);
        return ResponseEntity.status(HttpStatus.FOUND) // 302
                .location(new URI("https://www.giftipie.me/"))
                .body(baseResponse); // 2000
    }

    // 5. 로그인 사용자 정보 조회(내부용)
    @GetMapping("/user-info")
    public void getUserInfo(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        userService.getUserInfo(userDetails);
    }
}