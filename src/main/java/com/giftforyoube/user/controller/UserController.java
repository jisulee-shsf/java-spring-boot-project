package com.giftforyoube.user.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.giftforyoube.global.exception.BaseResponse;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.global.security.UserDetailsImpl;
import com.giftforyoube.user.dto.DeleteRequestDto;
import com.giftforyoube.user.dto.LoginResponseDto;
import com.giftforyoube.user.dto.SignupRequestDto;
import com.giftforyoube.user.service.GoogleUserService;
import com.giftforyoube.user.service.KakaoUserService;
import com.giftforyoube.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "유저", description = "유저 관련 API")
public class UserController {

    private final UserService userService;
    private final KakaoUserService kakaoUserService;
    private final GoogleUserService googleUserService;

    // 1. 회원가입
    @PostMapping("/signup")
    public ResponseEntity<BaseResponse<Void>> registerAccount(@Valid @RequestBody SignupRequestDto requestDto,
                                                              BindingResult bindingResult) throws MethodArgumentNotValidException {
        userService.registerAccount(requestDto, bindingResult);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new BaseResponse<>(BaseResponseStatus.REGISTER_ACCOUNT_SUCCESS));
    }

    // 2-1. 카카오 로그인
    @GetMapping("/kakao/callback")
    public ResponseEntity<BaseResponse<LoginResponseDto>> kakaoLogin(@RequestParam String code) throws JsonProcessingException {
        LoginResponseDto loginResponseDto = kakaoUserService.kakaoLogin(code);

        return ResponseEntity.status(HttpStatus.OK)
                .body(new BaseResponse<>(BaseResponseStatus.KAKAO_LOGIN_SUCCESS, loginResponseDto));
    }

    // 2-2. 구글 로그인
    @GetMapping("/login/oauth2/code/google")
    public ResponseEntity<BaseResponse<LoginResponseDto>> googleLogin(@RequestParam String code) throws JsonProcessingException {
        LoginResponseDto loginResponseDto = googleUserService.googleLogin(code);

        return ResponseEntity.status(HttpStatus.OK)
                .body(new BaseResponse<>(BaseResponseStatus.GOOGLE_LOGIN_SUCCESS, loginResponseDto));
    }

    // 3. 로그인 사용자 정보 조회(내부 테스트용)
    @GetMapping("/user-info/test")
    public void getUserInfoAfterLogin(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        log.info("[userDetails] getUser().getEmail(): " + userDetails.getUser().getEmail());
        log.info("[userDetails] getUser().getUserType(): " + userDetails.getUser().getUserType());
    }

    // 4. 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<Void>> logout(HttpServletRequest httpServletRequest) {
        userService.logout(httpServletRequest);

        return ResponseEntity.status(HttpStatus.OK)
                .body(new BaseResponse<>(BaseResponseStatus.LOGOUT_SUCCESS));
    }

    // 5. 회원탈퇴
    @DeleteMapping("/delete")
    public ResponseEntity<BaseResponse<Void>> deleteAccount(@Valid @RequestBody DeleteRequestDto deleteRequestDto,
                                                            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        userService.deleteAccount(userDetails.getUser().getId(), deleteRequestDto.getPassword());

        return ResponseEntity.status(HttpStatus.OK)
                .body(new BaseResponse<>(BaseResponseStatus.DELETE_ACCOUNT_SUCCESS));
    }
}