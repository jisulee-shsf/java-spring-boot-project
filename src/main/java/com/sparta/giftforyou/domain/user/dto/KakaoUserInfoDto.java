package com.sparta.giftforyou.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoUserInfoDto {
    private Long kakaoId;
    private String nickname;
    private String email;

    public KakaoUserInfoDto(Long kakaoId, String nickname, String email) {
        this.kakaoId = kakaoId;
        this.nickname = nickname;
        this.email = email;
    }
}