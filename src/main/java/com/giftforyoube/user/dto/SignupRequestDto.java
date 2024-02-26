package com.giftforyoube.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupRequestDto {

    @NotBlank(message = "이메일이 입력되지 않았습니다. 이메일을 입력해 주세요.")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "유효한 이메일 형식이어야 합니다.")
    private String email;

    @NotBlank(message = "닉네임이 입력되지 않았습니다. 닉네임을 입력해 주세요.")
    private String nickname;

    @NotBlank(message = "비밀번호가 입력되지 않았습니다. 비밀번호를 입력해 주세요.")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?_~(),])[A-Za-z\\d@$!%*?_~(),]{8,15}$",
            message = "비밀번호는 8자에서 15자 사이의 알파벳 대소문자, 숫자, 특수문자로 구성되어야 합니다.")
    private String password;

    private Boolean isEmailNotificationAgreed;
}