package com.sparta.giftforyou.domain.user.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sparta.giftforyou.domain.user.dto.SignupRequestDto;
import com.sparta.giftforyou.domain.user.entity.User;
import com.sparta.giftforyou.domain.user.service.KakaoService;
import com.sparta.giftforyou.domain.user.service.UserService;
import com.sparta.giftforyou.global.jwt.JwtUtil;
import com.sparta.giftforyou.global.security.UserDetailsImpl;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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

    public UserController(UserService userService, KakaoService kakaoService) {
        this.userService = userService;
        this.kakaoService = kakaoService;
    }

    // 회원 가입
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody @Valid SignupRequestDto requestDto, BindingResult bindingResult) {
        return userService.signup(requestDto, bindingResult);
    }

    // 카카오 로그인
    @GetMapping("/kakao/callback")
    public String kakaoLogin(@RequestParam String code, HttpServletResponse response) throws JsonProcessingException {
        String token = kakaoService.kakaoLogin(code);
        String tokenValue = JwtUtil.addJwtToCookie(token, response);
        Cookie cookie = new Cookie(JwtUtil.AUTHORIZATION_HEADER, tokenValue);
        cookie.setPath("/");
        response.addCookie(cookie);
        return "redirect:/";
    }

    // 사용자 정보 조회 테스트
    @GetMapping("/users")
    public void getUserInfoAfterLogin(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = userDetails.getUser();
        log.info("[userDetails] getUsername(): " + userDetails.getUsername());
        log.info("[userDetails] getPassword(): " + userDetails.getPassword());
        log.info("[userDetails] getUser().getId(): " + userDetails.getUser().getId());
        log.info("[userDetails] getUser().getEmail(): " + userDetails.getUser().getEmail());
        log.info("[userDetails] getUser().getPhoneNumber(): " + userDetails.getUser().getPhoneNumber());
        log.info("[userDetails] getUser().getKakaoId(): " + userDetails.getUser().getKakaoId());
    }
}