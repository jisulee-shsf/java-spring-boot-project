package com.sparta.giftforyou.domain.user.dto;

import lombok.Getter;
@Getter
public class LoginSuccessResponseDto {
    private String valueToken;

    public LoginSuccessResponseDto(String valueToken) {
        this.valueToken = valueToken;
    }
}