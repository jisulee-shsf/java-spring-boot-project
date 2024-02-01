package com.giftforyoube.user.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.giftforyoube.global.jwt.JwtUtil;
import com.giftforyoube.global.security.UserDetailsImpl;
import com.giftforyoube.user.dto.MsgResponseDto;
import com.giftforyoube.user.dto.SignupRequestDto;
import com.giftforyoube.user.entity.User;
import com.giftforyoube.user.service.GoogleService;
import com.giftforyoube.user.service.KakaoService;
import com.giftforyoube.user.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class UserController {
    private final UserService userService;
    private final KakaoService kakaoService;
    private final GoogleService googleService;

    public UserController(UserService userService, KakaoService kakaoService, GoogleService googleService) {
        this.userService = userService;
        this.kakaoService = kakaoService;
        this.googleService = googleService;
    }

    // 회원가입
    @PostMapping("/signup")
    public MsgResponseDto signup(@RequestBody @Valid SignupRequestDto requestDto, BindingResult bindingResult) {
        return userService.signup(requestDto, bindingResult);
    }

    // 로그인(Kakao)
    @GetMapping("/kakao/callback")
    public String kakaoLogin(@RequestParam String code, HttpServletResponse response) throws JsonProcessingException {
        String kakaoToken = kakaoService.kakaoLogin(code);
        String kakaoTokenValue = JwtUtil.addJwtToCookie(kakaoToken, response);
        Cookie cookie = new Cookie(JwtUtil.AUTHORIZATION_HEADER, kakaoTokenValue);
        cookie.setPath("/");
        response.addCookie(cookie);
        return "redirect:/";
    }

    // 로그인(Google)
    @GetMapping("/login/login/oauth2/code/google")
    public String googleLogin(@RequestParam String code, HttpServletResponse response) throws JsonProcessingException {
        String googleToken = googleService.googleLogin(code);
        String googleTokenValue = JwtUtil.addJwtToCookie(googleToken, response);
        Cookie cookie = new Cookie(JwtUtil.AUTHORIZATION_HEADER, googleTokenValue);
        cookie.setPath("/");
        response.addCookie(cookie);
        return "redirect:/";
    }

    // 회원 탈퇴
    @DeleteMapping("/signout/{userId}")
    public MsgResponseDto signout(@PathVariable Long userId) {
        return userService.signout(userId);
    }

    // userDetails 조회용
    @GetMapping("/user-info")
    public void getUserInfoAfterLogin(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = userDetails.getUser();
        log.info("[userDetails] getUser().getId(): " + userDetails.getUser().getId());
        log.info("[userDetails] getUser().getEmail(): " + userDetails.getUser().getEmail());
        log.info("[userDetails] getPassword(): " + userDetails.getPassword());
        log.info("[userDetails] getNickname(): " + userDetails.getUsername());
        log.info("[userDetails] getUser().getPhoneNumber(): " + userDetails.getUser().getPhoneNumber());
        log.info("[userDetails] getUser().getKakaoId(): " + userDetails.getUser().getKakaoId());
        log.info("[userDetails] getUser().getGoogleId(): " + userDetails.getUser().getGoogleId());
    }
}