package com.giftforyoube.user.service;

import com.giftforyoube.global.exception.BaseException;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.global.jwt.dto.JwtTokenInfo;
import com.giftforyoube.global.jwt.util.JwtTokenUtil;
import com.giftforyoube.user.dto.SignupRequestDto;
import com.giftforyoube.user.entity.User;
import com.giftforyoube.user.entity.UserType;
import com.giftforyoube.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtTokenUtil jwtTokenUtil;

    /**
     * 1. 회원가입
     *
     * @param signupRequestDto 회원가입 요청 DTO
     * @param bindingResult 입력값 유효성 검증 결과
     * @throws MethodArgumentNotValidException 입력값이 유효하지 않을 경우
     * @throws BaseException 이미 존재하는 이메일로 회원가입을 시도할 경우
     */
    @Transactional
    public void registerAccount(SignupRequestDto signupRequestDto, BindingResult bindingResult)
            throws MethodArgumentNotValidException {
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
    }

    /**
     * 2. 로그아웃
     *
     * @param user 로그아웃할 유저 객체
     * @param httpServletResponse HTTP 응답 객체
     */
    @Transactional
    public void logout(User user, HttpServletResponse httpServletResponse) {
        // 쿠키 내 액세스 토큰 삭제
        Cookie removedTokenCookie = jwtTokenUtil.removeTokenCookie();
        httpServletResponse.addCookie(removedTokenCookie);

        // 액세스 및 리프레시 토큰 유효시간 만료
        user.expireAccessTokenExpirationTime(LocalDateTime.now());
        user.expireRefreshTokenExpirationTime(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * 3. 회원탈퇴
     *
     * @param user 삭제할 유저 객체
     * @param inputPassword 입력된 비밀번호
     * @param httpServletResponse HTTP 응답 객체
     * @throws BaseException 비밀번호가 일치하지 않을 경우
     */
    @Transactional
    public void deleteAccount(User user, String inputPassword, HttpServletResponse httpServletResponse) {
        // 비밀번호 일치 여부 확인
        if (!passwordEncoder.matches(inputPassword, user.getPassword())) {
            throw new BaseException(BaseResponseStatus.PASSWORD_MISMATCH);
        }

        // 쿠키 내 액세스 토큰 삭제
        Cookie removedTokenCookie = jwtTokenUtil.removeTokenCookie();
        httpServletResponse.addCookie(removedTokenCookie);
        userRepository.delete(user);
    }

    /**
     * 4-1. 액세스 토큰 정보 업데이트
     *
     * @param user 토큰 정보를 업데이트할 유저 객체
     * @param accessTokenInfo 업데이트할 액세스 토큰 정보
     */
    @Transactional
    public void updateAccessToken(User user, JwtTokenInfo.AccessTokenInfo accessTokenInfo) {
        user.updateAccessTokenInfo(accessTokenInfo);
        userRepository.save(user);
    }

    /**
     * 4-2. 리프레시 토큰 정보 업데이트
     *
     * @param user 토큰 정보를 업데이트할 유저 객체
     * @param refreshTokenInfo 업데이트할 리프레시 토큰 정보
     */
    @Transactional
    public void updateRefreshToken(User user, JwtTokenInfo.RefreshTokenInfo refreshTokenInfo) {
        user.updateRefreshTokenInfo(refreshTokenInfo);
        userRepository.save(user);
    }

    /**
     * 5. 액세스 토큰 기반 유저 확인
     *
     * @param accessToken 유저의 액세스 토큰
     * @return 주어진 액세스 토큰을 가진 유저
     * @throws BaseException 주어진 액세스 토큰을 가진 유저가 없을 경우
     */
    @Transactional(readOnly = true)
    public User findUserByAccessToken(String accessToken) {
        return userRepository.findUserByAccessToken(accessToken)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.TOKEN_NOT_FOUND));
    }

    /**
     * 6. 리프레시 토큰 유효 여부 확인
     *
     * @param refreshToken 유효 여부를 확인할 리프레시 토큰
     * @return 리프레시 토큰의 유효 여부(유효한 경우 true, 만료된 경우 false)
     */
    @Transactional(readOnly = true)
    public boolean isRefreshTokenValid(String refreshToken) {
        LocalDateTime refreshTokenExpirationTime = userRepository
                .findRefreshTokenExpirationTimeByRefreshToken(refreshToken);
        return !refreshTokenExpirationTime.isBefore(LocalDateTime.now());
    }
}