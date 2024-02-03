package com.giftforyoube.user.service;

import com.giftforyoube.global.exception.BaseException;
import com.giftforyoube.global.exception.BaseResponse;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.global.security.UserDetailsImpl;
import com.giftforyoube.user.dto.SignupRequestDto;
import com.giftforyoube.user.entity.User;
import com.giftforyoube.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 회원가입
    @Transactional
    public ResponseEntity<BaseResponse<Void>> registerAccount(SignupRequestDto signupRequestDto, BindingResult bindingResult) throws MethodArgumentNotValidException {
        log.info("[registerAccount] 회원가입 시도");

        // 1. 유효성 검사
        if (bindingResult.hasErrors()) {
            throw new MethodArgumentNotValidException(null, bindingResult);
        }

        // 2. 중복 이메일 검사
        if (userRepository.findByEmail(signupRequestDto.getEmail()).isPresent()) {
            throw new BaseException(BaseResponseStatus.EMAIL_ALREADY_EXISTS);
        }

        // 3. 중복 휴대전화 번호 검사
        if (userRepository.findByPhoneNumber(signupRequestDto.getPhoneNumber()).isPresent()) {
            throw new BaseException(BaseResponseStatus.PHONENUMBER_ALREADY_EXISTS);
        }

        // 4. 회원가입 진행
        String encryptedPassword = passwordEncoder.encode(signupRequestDto.getPassword());
        User user = new User(signupRequestDto.getEmail(), encryptedPassword, signupRequestDto.getNickname(), signupRequestDto.getPhoneNumber());
        userRepository.save(user);

        log.info("[registerAccount] 회원가입 완료(REGISTER_ACCOUNT_SUCCESS)");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new BaseResponse<>(BaseResponseStatus.REGISTER_ACCOUNT_SUCCESS));
    }

    @Transactional
    // 회원탈퇴
    public BaseResponse<Void> deleteAccount(Long userId, String inputPassword) {
        log.info("[deleteAccount] 회원탈퇴 시도");

        // 1. 사용자 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.USER_NOT_FOUND));

        // 2. 비밀번호 확인
        if (!passwordEncoder.matches(inputPassword, user.getPassword())) {
            throw new BaseException(BaseResponseStatus.PASSWORD_MISMATCH);
        }

        // 3. 회원탈퇴 진행
        userRepository.delete(user);
        log.info("[deleteAccount] 회원탈퇴 완료");
        return new BaseResponse<>(BaseResponseStatus.DELETE_ACCOUNT_SUCCESS);
    }

    // 회원정보 조회(내부 확인용)
    public void getUserInfo(UserDetailsImpl userDetails) {
        log.info("[getUserInfo] 회원정보 조회 시도");
        User user = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new BaseException(BaseResponseStatus.USER_NOT_FOUND));
        log.info("[userDetails] getUser().getId(): " + userDetails.getUser().getId());
        log.info("[userDetails] getUser().getEmail(): " + userDetails.getUser().getEmail());
        log.info("[userDetails] getPassword(): " + userDetails.getPassword());
        log.info("[userDetails] getNickname(): " + userDetails.getUsername());
        log.info("[userDetails] getUser().getPhoneNumber(): " + userDetails.getUser().getPhoneNumber());
        log.info("[userDetails] getUser().getKakaoId(): " + userDetails.getUser().getKakaoId());
        log.info("[userDetails] getUser().getGoogleId(): " + userDetails.getUser().getGoogleId());
        log.info("[getUserInfo] 회원정보 조회 완료");
    }
}