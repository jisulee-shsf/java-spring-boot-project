package com.giftforyoube.user.controller;

import com.giftforyoube.global.exception.BaseException;
import com.giftforyoube.global.exception.BaseResponse;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.global.security.UserDetailsImpl;
import com.giftforyoube.user.dto.DeleteRequestDto;
import com.giftforyoube.user.dto.SignupRequestDto;
import com.giftforyoube.user.service.GoogleUserService;
import com.giftforyoube.user.service.KakaoUserService;
import com.giftforyoube.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
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
                                                              BindingResult bindingResult)
            throws MethodArgumentNotValidException {
        userService.registerAccount(requestDto, bindingResult);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new BaseResponse<>(BaseResponseStatus.REGISTER_ACCOUNT_SUCCESS));
    }

    // 2-1. 카카오 로그인
    @GetMapping("/kakao/callback")
    public ResponseEntity<BaseResponse<Void>> kakaoLogin(@RequestParam String code,
                                                         HttpServletResponse httpServletResponse) {
        try {
            kakaoUserService.kakaoLogin(code, httpServletResponse);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new BaseResponse<>(BaseResponseStatus.KAKAO_LOGIN_SUCCESS));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new BaseResponse<>(BaseResponseStatus.KAKAO_LOGIN_FAILED));
        }
    }

    // 2-2. 구글 로그인
    @GetMapping("/google/login")
    public ResponseEntity<BaseResponse<Void>> googleLogin(@RequestParam String code,
                                                          HttpServletResponse httpServletResponse) {
        try {
            googleUserService.googleLogin(code, httpServletResponse);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new BaseResponse<>(BaseResponseStatus.GOOGLE_LOGIN_SUCCESS));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new BaseResponse<>(BaseResponseStatus.GOOGLE_LOGIN_FAILED));
        }
    }

    // 3. 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<Void>> logout(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                     HttpServletResponse httpServletResponse) {
        try {
            userService.logout(userDetails.getUser(), httpServletResponse);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new BaseResponse<>(BaseResponseStatus.LOGOUT_SUCCESS));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BaseResponse<>(BaseResponseStatus.LOGOUT_FAILED));
        }
    }

    // 4. 회원탈퇴
    @DeleteMapping("/delete")
    public ResponseEntity<BaseResponse<Void>> deleteAccount(@Valid @RequestBody DeleteRequestDto deleteRequestDto,
                                                            @AuthenticationPrincipal UserDetailsImpl userDetails,
                                                            HttpServletResponse httpServletResponse) {
        try {
            userService.deleteAccount(userDetails.getUser(), deleteRequestDto.getPassword(), httpServletResponse);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new BaseResponse<>(BaseResponseStatus.DELETE_ACCOUNT_SUCCESS));
        } catch (BaseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new BaseResponse<>(BaseResponseStatus.DELETE_ACCOUNT_FAILED));
        }
    }

    // [Test] 유저 정보 조회
    @GetMapping("/user-info")
    public void getUserInfoForTest(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        userService.getUserInfo(userDetails.getUser().getEmail());
    }
}