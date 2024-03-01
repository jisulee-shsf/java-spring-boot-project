package com.giftforyoube.user.service;

import com.giftforyoube.global.exception.BaseException;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.global.jwt.constant.TokenType;
import com.giftforyoube.global.jwt.token.service.TokenManager;
import com.giftforyoube.global.jwt.util.AuthorizationHeaderUtil;
import com.giftforyoube.user.dto.SignupRequestDto;
import com.giftforyoube.user.entity.User;
import com.giftforyoube.user.entity.UserType;
import com.giftforyoube.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final TokenManager tokenManager;

    // 1. 회원가입
    public void registerAccount(SignupRequestDto signupRequestDto,
                                BindingResult bindingResult) throws MethodArgumentNotValidException {
        log.info("[registerAccount] 회원가입 시도");

        // 가입정보 유효성 검증
        if (bindingResult.hasErrors()) {
            throw new MethodArgumentNotValidException(null, bindingResult);
        }

        // 이메일 중복 여부 확인
        if (userRepository.findByEmail(signupRequestDto.getEmail()).isPresent()) {
            throw new BaseException(BaseResponseStatus.EMAIL_ALREADY_EXISTS);
        }

        // 회원가입 진행
        String encryptedPassword = passwordEncoder.encode(signupRequestDto.getPassword());
        User user = User.builder()
                .email(signupRequestDto.getEmail())
                .password(encryptedPassword)
                .nickname(signupRequestDto.getNickname())
                .isEmailNotificationAgreed(signupRequestDto.getIsEmailNotificationAgreed())
                .userType(UserType.USER)
                .build();
        userRepository.save(user);

        log.info("[registerAccount] 회원가입 완료");
    }

    // 2. 로그아웃
    public void logout(HttpServletRequest httpServletRequest) {
        log.info("[logout] 로그아웃 시도");

        // Authorization 헤더 검증
        String authorizationHeader = httpServletRequest.getHeader("Authorization");
        AuthorizationHeaderUtil.validateAuthorization(authorizationHeader);

        // 토큰 검증
        String accessToken = authorizationHeader.split(" ")[1];
        tokenManager.validateToken(accessToken);

        // 토큰 타입 확인
        Claims tokenClaims = tokenManager.getTokenClaims(accessToken);
        String tokenType = tokenClaims.getSubject();
        if (!TokenType.isAccessToken(tokenType)) {
            throw new BaseException(BaseResponseStatus.INVALID_ACCESS_TOKEN_TYPE);
        }

        // 리프레시 토큰 만료 처리
        String email = (String) tokenClaims.get("email");
        User user = findUserByEmail(email);
        user.expireRefreshToken(LocalDateTime.now());
        userRepository.save(user);

        log.info("[logout] 로그아웃 완료");
    }

    // 3. 회원탈퇴
    public void deleteAccount(Long userId, String inputPassword) {
        log.info("[deleteAccount] 회원탈퇴 시도");

        // 해당 userId를 가진 사용자 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.USER_NOT_FOUND));

        // 비밀번호 일치 여부 확인
        if (!passwordEncoder.matches(inputPassword, user.getPassword())) {
            throw new BaseException(BaseResponseStatus.PASSWORD_MISMATCH);
        }

        // 회원탈퇴 진행
        userRepository.delete(user);

        log.info("[deleteAccount] 회원탈퇴 완료");
    }

    // 4. 리프레시 토큰으로 사용자 찾기
    @Transactional(readOnly = true)
    public User findUserByRefreshToken(String refreshToken) {

        // 해당 리프레시 토큰을 가진 사용자 확인
        User user = userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new BaseException((BaseResponseStatus.REFRESH_TOKEN_NOT_FOUND)));

        // 사용자가 가진 리프레시 토큰 만료 여부 확인
        LocalDateTime tokenExpirationTime = user.getTokenExpirationTime();
        if (tokenExpirationTime.isBefore(LocalDateTime.now())) {
            throw new BaseException(BaseResponseStatus.REFRESH_TOKEN_EXPIRED);
        }

        return user;
    }

    // 5. 이메일로 사용자 찾기
    @Transactional(readOnly = true)
    public User findUserByEmail(String email) {

        // 해당 이메일을 가진 사용자 확인
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.USER_NOT_FOUND));
    }
}