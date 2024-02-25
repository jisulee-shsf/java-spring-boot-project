package com.giftforyoube.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.giftforyoube.global.jwt.dto.JwtTokenDto;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {

    private String grantType;
    private String accessToken;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private Date accessTokenExpireTime;
    private String refreshToken;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private Date refreshTokenExpireTime;

    public LoginResponseDto(JwtTokenDto jwtTokenDto) {
        this.grantType = jwtTokenDto.getGrantType();
        this.accessToken = jwtTokenDto.getAccessToken();
        this.accessTokenExpireTime = jwtTokenDto.getAccessTokenExpireTime();
        this.refreshToken = jwtTokenDto.getRefreshToken();
        this.refreshTokenExpireTime = jwtTokenDto.getRefreshTokenExpireTime();
    }
}