package com.giftforyoube.user.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.giftforyoube.global.exception.BaseResponse;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.global.security.UserDetailsImpl;
import com.giftforyoube.user.dto.DeleteRequestDto;
import com.giftforyoube.user.dto.LoginResponseDto;
import com.giftforyoube.user.dto.SignupRequestDto;
import com.giftforyoube.user.service.GoogleService;
import com.giftforyoube.user.service.KakaoService;
import com.giftforyoube.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "유저", description = "유저 관련 API")
public class UserController {

    private final UserService userService;
    private final KakaoService kakaoService;
    private final GoogleService googleService;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<BaseResponse<Void>> registerAccount(@Valid @RequestBody SignupRequestDto requestDto,
                                                              BindingResult bindingResult) throws MethodArgumentNotValidException {
        userService.registerAccount(requestDto, bindingResult);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new BaseResponse<>(BaseResponseStatus.REGISTER_ACCOUNT_SUCCESS));
    }

    // 회원탈퇴
    @DeleteMapping("/delete")
    public ResponseEntity<BaseResponse<Void>> deleteAccount(@Valid @RequestBody DeleteRequestDto deleteRequestDto,
                                                            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        userService.deleteAccount(userDetails.getUser().getId(), deleteRequestDto.getPassword());
        return ResponseEntity.status(HttpStatus.OK)
                .body(new BaseResponse<>(BaseResponseStatus.DELETE_ACCOUNT_SUCCESS));
    }

    // 카카오 로그인
    @GetMapping("/kakao/callback")
    public ResponseEntity<BaseResponse<LoginResponseDto>> kakaoLogin(@RequestParam String code) throws JsonProcessingException {
        LoginResponseDto loginResponseDto = kakaoService.kakaoLogin(code);
        return ResponseEntity.status(HttpStatus.OK)
                .body(new BaseResponse<>(BaseResponseStatus.KAKAO_LOGIN_SUCCESS, loginResponseDto));
    }

    // 구글 로그인
    @GetMapping("/login/oauth2/code/google")
    public ResponseEntity<BaseResponse<LoginResponseDto>> googleLogin(@RequestParam String code) throws JsonProcessingException {
        LoginResponseDto loginResponseDto = googleService.googleLogin(code);
        return ResponseEntity.status(HttpStatus.OK)
                .body(new BaseResponse<>(BaseResponseStatus.GOOGLE_LOGIN_SUCCESS, loginResponseDto));
    }
}