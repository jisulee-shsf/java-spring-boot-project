package com.giftforyoube.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DeleteRequestDto {

    @NotBlank(message = "비밀번호가 입력되지 않았습니다. 비밀번호를 입력해 주세요.")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?_~(),])[A-Za-z\\d@$!%*?_~(),]{8,15}$",
            message = "비밀번호는 8자에서 15자 사이의 알파벳 대소문자, 숫자, 특수문자로 구성되어야 합니다.")
    private String password;

    public DeleteRequestDto(String password) {
        this.password = password;
    }
}