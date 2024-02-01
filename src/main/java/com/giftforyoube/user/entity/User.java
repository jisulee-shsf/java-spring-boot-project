package com.giftforyoube.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;

    @Column
    private String password;

    @Column
    private String nickname;

    @Column(unique = true)
    private String phoneNumber;

    @Column
    private Long kakaoId = 0L;

    @Column
    private String kakaoAccessToken;

    public User(String email, String password, String nickname, String phoneNumber) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.phoneNumber = phoneNumber;
    }

    public User(String email, String password, String nickname, Long kakaoId, String kakaoAccessToken, String phoneNumber) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.kakaoId = kakaoId;
        this.kakaoAccessToken = kakaoAccessToken;
        this.phoneNumber = phoneNumber;
    }

    public User kakaoIdAndAccessTokenUpdate(Long kakaoId, String kakaoAccessToken) {
        this.kakaoId = kakaoId;
        this.kakaoAccessToken = kakaoAccessToken;
        return this;
    }

    public User kakaoAccessTokenUpdate(String kakaoAccessToken) {
        this.kakaoAccessToken = kakaoAccessToken;
        return this;
    }
}