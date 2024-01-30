package com.giftforyoube.user.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.giftforyoube.user.dto.SignupRequestDto;
import com.giftforyoube.user.entity.User;
import com.giftforyoube.user.service.UserService;
import com.giftforyoube.user.dto.MsgResponseDto;
import com.giftforyoube.user.service.KakaoService;
import com.giftforyoube.global.jwt.JwtUtil;
import com.giftforyoube.global.security.UserDetailsImpl;
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

    public UserController(UserService userService, KakaoService kakaoService) {
        this.userService = userService;
        this.kakaoService = kakaoService;
    }

    // 회원가입
    @PostMapping("/signup")
    public MsgResponseDto signup(@RequestBody @Valid SignupRequestDto requestDto, BindingResult bindingResult) {
        return userService.signup(requestDto, bindingResult);
    }

    // 로그인(Kakao)
    @GetMapping("/kakao/callback")
    public String kakaoLogin(@RequestParam String code, HttpServletResponse response) throws JsonProcessingException {
        String token = kakaoService.kakaoLogin(code);
        String tokenValue = JwtUtil.addJwtToCookie(token, response);
        Cookie cookie = new Cookie(JwtUtil.AUTHORIZATION_HEADER, tokenValue);
        cookie.setPath("/");
        response.addCookie(cookie);
        return "redirect:http://localhost:8080"; // 연동 테스트 후 업데이트 예정(1/29~)

    }

    // 사용자 정보 조회 테스트
    @GetMapping("/user-info")
    public void getUserInfoAfterLogin(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = userDetails.getUser();
        log.info("[userDetails] getUser().getId(): " + userDetails.getUser().getId());
        log.info("[userDetails] getUser().getEmail(): " + userDetails.getUser().getEmail());
        log.info("[userDetails] getPassword(): " + userDetails.getPassword());
        log.info("[userDetails] getNickname(): " + userDetails.getUsername());
        log.info("[userDetails] getUser().getPhoneNumber(): " + userDetails.getUser().getPhoneNumber());
        log.info("[userDetails] getUser().getKakaoId(): " + userDetails.getUser().getKakaoId());
    }
}