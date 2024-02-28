package com.giftforyoube.user.service;

import com.giftforyoube.global.exception.BaseException;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.user.dto.SignupRequestDto;
import com.giftforyoube.user.entity.User;
import com.giftforyoube.user.entity.UserType;
import com.giftforyoube.user.repository.UserRepository;
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
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void registerAccount(SignupRequestDto signupRequestDto,
                                BindingResult bindingResult) throws MethodArgumentNotValidException {
        log.info("[registerAccount] 회원가입 시도");

        // 1. 유효성 검사
        if (bindingResult.hasErrors()) {
            throw new MethodArgumentNotValidException(null, bindingResult);
        }

        // 2. 이메일 중복 여부 확인
        if (userRepository.findByEmail(signupRequestDto.getEmail()).isPresent()) {
            throw new BaseException(BaseResponseStatus.EMAIL_ALREADY_EXISTS);
        }

        // 3. 회원가입 진행
        String encryptedPassword = passwordEncoder.encode(signupRequestDto.getPassword());
        User user = new User(signupRequestDto.getEmail(), encryptedPassword, signupRequestDto.getNickname(), signupRequestDto.getIsEmailNotificationAgreed(), UserType.USER);
        userRepository.save(user);
        log.info("[registerAccount] 회원가입 완료");
    }

    @Transactional
    public void deleteAccount(Long userId, String inputPassword) {
        log.info("[deleteAccount] 회원탈퇴 시도");

        // 1. 사용자 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.NOT_FOUND_USER));

        // 2. 비밀번호 확인
        if (!passwordEncoder.matches(inputPassword, user.getPassword())) {
            throw new BaseException(BaseResponseStatus.PASSWORD_MISMATCH);
        }

        // 3. 회원탈퇴 진행
        userRepository.delete(user);
        log.info("[deleteAccount] 회원탈퇴 완료");
    }

    @Transactional(readOnly = true)
    public User findUserByRefreshToken(String refreshToken) {
        User user = userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new BaseException((BaseResponseStatus.REFRESH_TOKEN_NOT_FOUND)));
        LocalDateTime tokenExpirationTime = user.getTokenExpirationTime();
        if (tokenExpirationTime.isBefore(LocalDateTime.now())) {
            throw new BaseException(BaseResponseStatus.REFRESH_TOKEN_EXPIRED);
        }
        return user;
    }
}