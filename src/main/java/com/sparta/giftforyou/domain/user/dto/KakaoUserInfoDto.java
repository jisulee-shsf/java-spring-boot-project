package com.sparta.giftforyou.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoUserInfoDto {
    private Long kakaoId;
    private String username;
    private String email;

    public KakaoUserInfoDto(Long kakaoId, String username, String email) {
        this.kakaoId = kakaoId;
        this.username = username;
        this.email = email;
    }
}