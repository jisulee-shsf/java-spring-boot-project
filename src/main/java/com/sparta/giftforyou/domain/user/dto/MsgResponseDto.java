package com.sparta.giftforyou.domain.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MsgResponseDto {
    private String data = "";
    private int status;
    private String msg;
    private Long kakaoId = 0L;

    public MsgResponseDto(String data, int status, String msg) {
        this.data = data;
        this.status = status;
        this.msg = msg;
    }

    public MsgResponseDto(int status, String msg) {
        this.status = status;
        this.msg = msg;
    }

    public MsgResponseDto(int status, String msg, Long kakaoId) {
        this.status = status;
        this.msg = msg;
        this.kakaoId = kakaoId;
    }
}

