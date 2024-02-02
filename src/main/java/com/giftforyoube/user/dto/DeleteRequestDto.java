package com.giftforyoube.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DeleteRequestDto {
    private String password;

    public DeleteRequestDto(String password) {
        this.password = password;
    }
}