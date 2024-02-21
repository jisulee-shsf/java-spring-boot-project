package com.giftforyoube.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleUserInfoDto {

    private String id;
    private String name;
    private String email;

    public GoogleUserInfoDto(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }
}