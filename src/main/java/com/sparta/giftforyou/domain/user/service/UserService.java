package com.sparta.giftforyou.domain.user.service;

import com.sparta.giftforyou.domain.user.dto.SignupRequestDto;
import com.sparta.giftforyou.domain.user.entity.User;
import com.sparta.giftforyou.domain.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public ResponseEntity<String> signup(SignupRequestDto requestDto, BindingResult bindingResult) {
        log.info("[signup] 회원가입 시도");

        // 회원가입 정보 유효성 검사
        List<FieldError> fieldErrors = bindingResult.getFieldErrors();
        if (fieldErrors.size() > 0) {
            for (FieldError fieldError : bindingResult.getFieldErrors()) {
                log.error("[signup] 회원가입 유효성 검사 실패: " + fieldError.getDefaultMessage());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldError.getDefaultMessage());
            }
        }

        // 이메일 중복 검사
        userRepository.findByEmail(requestDto.getEmail())
                .ifPresent(user -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 가입된 이메일입니다.");
                });

        // 휴대폰 번호 중복 검사
        userRepository.findByPhoneNumber(requestDto.getPhoneNumber())
                .ifPresent(user -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 가입된 휴대폰 번호입니다.");
                });

        // 패스워드 암호화 및 회원가입 정보 등록
        String encryptedPassword = passwordEncoder.encode(requestDto.getPassword());
        User user = new User(requestDto.getEmail(), encryptedPassword, requestDto.getNickname(), requestDto.getPhoneNumber());
        userRepository.save(user);
        log.info("[signup] 회원가입 완료");
        return ResponseEntity.status(HttpStatus.CREATED).body("회원가입이 완료되었습니다.");
    }
}