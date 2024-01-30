package com.giftforyoube.global.security;

import com.giftforyoube.user.dto.MsgResponseDto;
import com.giftforyoube.user.entity.User;
import com.giftforyoube.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomAuthenticationException(
                        new MsgResponseDto(HttpStatus.UNAUTHORIZED.value(), "가입된 사용자 정보가 없습니다.")
                ));
        return new UserDetailsImpl(user);
    }

    public class CustomAuthenticationException extends AuthenticationException {
        private final MsgResponseDto msgResponseDto;

        public CustomAuthenticationException(MsgResponseDto msgResponseDto) {
            super(msgResponseDto.getMsg());
            this.msgResponseDto = msgResponseDto;
        }
        public MsgResponseDto getMsgResponseDto() {
            return msgResponseDto;
        }
    }
}