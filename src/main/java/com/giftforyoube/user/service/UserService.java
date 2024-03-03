package com.giftforyoube.user.service;

import com.giftforyoube.global.exception.BaseException;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.global.jwt.constant.TokenType;
import com.giftforyoube.global.jwt.dto.JwtTokenInfo;
import com.giftforyoube.global.jwt.util.JwtTokenUtil;
import com.giftforyoube.user.dto.SignupRequestDto;
import com.giftforyoube.user.entity.User;
import com.giftforyoube.user.entity.UserType;
import com.giftforyoube.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
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
    private final JwtTokenUtil jwtTokenUtil;

    // 1. 회원가입
    public void registerAccount(SignupRequestDto signupRequestDto, BindingResult bindingResult)
            throws MethodArgumentNotValidException {
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
    public void logout(User user, HttpServletResponse httpServletResponse) {
        log.info("[logout] 로그아웃 시도");

        // 쿠키 내 액세스 토큰 삭제
        Cookie removedTokenCookie = jwtTokenUtil.removeTokenCookie();
        httpServletResponse.addCookie(removedTokenCookie);

        // 액세스 및 리프레시 토큰 유효시간 만료
        expireToken(user, TokenType.ACCESS);
        expireToken(user, TokenType.REFRESH);
        userRepository.save(user);

        log.info("[logout] 로그아웃 완료");
    }

    // 3. 회원탈퇴
    public void deleteAccount(User user, String inputPassword, HttpServletResponse httpServletResponse) {
        log.info("[deleteAccount] 회원탈퇴 시도");

        // 비밀번호 일치 여부 확인
        if (!passwordEncoder.matches(inputPassword, user.getPassword())) {
            throw new BaseException(BaseResponseStatus.PASSWORD_MISMATCH);
        }

        // 쿠키 내 액세스 토큰 삭제
        Cookie removedTokenCookie = jwtTokenUtil.removeTokenCookie();
        httpServletResponse.addCookie(removedTokenCookie);
        userRepository.delete(user);

        log.info("[deleteAccount] 회원탈퇴 완료");
    }

    // 4. JWT 토큰 만료
    public void expireToken(User user, TokenType tokenType) {
        if (tokenType == TokenType.ACCESS) {
            user.expireAccessTokenExpirationTime(LocalDateTime.now());
        } else {
            user.expireRefreshTokenExpirationTime(LocalDateTime.now());
        }
    }

    // 5-1. 액세스 토큰 정보 업데이트
    public void updateAccessToken(User user, JwtTokenInfo.AccessTokenInfo accessTokenInfo) {
        user.updateAccessTokenInfo(accessTokenInfo);
        userRepository.save(user);
    }

    // 5-2. 리프레시 토큰 정보 업데이트
    public void updateRefreshToken(User user, JwtTokenInfo.RefreshTokenInfo refreshTokenInfo) {
        user.updateRefreshTokenInfo(refreshTokenInfo);
        userRepository.save(user);
    }

    // 6. 리프레시 토큰 유효 여부 확인
    @Transactional(readOnly = true)
    public boolean isRefreshTokenValid(String refreshToken) {
        LocalDateTime refreshTokenExpirationTime = userRepository
                .findRefreshTokenExpirationTimeByRefreshToken(refreshToken);
        return !refreshTokenExpirationTime.isBefore(LocalDateTime.now());
    }

    // 7-1. 이메일 기반 유저 확인
    @Transactional(readOnly = true)
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.USER_NOT_FOUND));
    }

    // 7-2. 액세스 토큰 기반 유저 확인
    @Transactional(readOnly = true)
    public User findUserByAccessToken(String accessToken) {
        return userRepository.findUserByAccessToken(accessToken)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.TOKEN_NOT_FOUND));
    }

    // [Test] 유저 정보 조회
    @Transactional(readOnly = true)
    public void getUserInfoForTest(String email) {
        User user = findUserByEmail(email);
        log.info("[getUserInfoAfterLogin] " + user.getUserType());
        log.info("[getUserInfoAfterLogin] " + user.getEmail());
        log.info("[getUserInfoAfterLogin] " + user.getAccessToken());
        log.info("[getUserInfoAfterLogin] " + user.getAccessTokenExpirationTime());
        log.info("[getUserInfoAfterLogin] " + user.getRefreshToken());
        log.info("[getUserInfoAfterLogin] " + user.getRefreshTokenExpirationTime());
    }
}