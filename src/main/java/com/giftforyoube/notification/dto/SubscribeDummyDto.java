package com.giftforyoube.notification.dto;

import lombok.Getter;

@Getter
public class SubscribeDummyDto {

    private String username;
//    private final String message = "SSE 구독 성공";
    public SubscribeDummyDto(String username) {
        this.username = username;
    }
}
