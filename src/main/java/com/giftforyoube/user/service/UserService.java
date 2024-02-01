package com.giftforyoube.user.service;

import com.giftforyoube.user.dto.MsgResponseDto;
import com.giftforyoube.user.dto.SignupRequestDto;
import com.giftforyoube.user.entity.User;
import com.giftforyoube.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public MsgResponseDto signup(SignupRequestDto requestDto, BindingResult bindingResult) {
        log.info("[signup] 회원가입 시도");

        // 회원가입 정보 유효성 검사
        List<FieldError> fieldErrors = bindingResult.getFieldErrors();
        if (fieldErrors.size() > 0) {
            for (FieldError fieldError : bindingResult.getFieldErrors()) {
                log.error("[signup] 회원가입 유효성 검사 실패: " + fieldError.getDefaultMessage());
                return new MsgResponseDto(HttpStatus.BAD_REQUEST.value(), fieldError.getDefaultMessage());
            }
        }

        // 이메일 중복 검사
        if (userRepository.findByEmail(requestDto.getEmail()).isPresent()) {
            return new MsgResponseDto(HttpStatus.BAD_REQUEST.value(), "이미 가입된 이메일입니다.");
        }

        // 휴대폰 번호 중복 검사
        if (userRepository.findByPhoneNumber(requestDto.getPhoneNumber()).isPresent()) {
            return new MsgResponseDto(HttpStatus.BAD_REQUEST.value(), "이미 가입된 휴대폰 번호입니다.");
        }

        // 패스워드 암호화 및 회원가입 정보 등록
        String encryptedPassword = passwordEncoder.encode(requestDto.getPassword());
        User user = new User(requestDto.getEmail(), encryptedPassword, requestDto.getNickname(), requestDto.getPhoneNumber());
        userRepository.save(user);

        log.info("[signup] 회원가입 완료");
        return new MsgResponseDto(HttpStatus.CREATED.value(), "회원가입이 완료되었습니다.");
    }

    public MsgResponseDto signout(Long userId) {
        log.info("[signout] 회원탈퇴 시도");
        User user = userRepository.findById(userId).orElseThrow(
                () -> new NoSuchElementException("회원탈퇴에 실패했습니다."));
        userRepository.delete(user);

        log.info("[signout] 회원탈퇴 완료");
        return new MsgResponseDto(HttpStatus.OK.value(), "회원탈퇴가 완료되었습니다.");
    }
}