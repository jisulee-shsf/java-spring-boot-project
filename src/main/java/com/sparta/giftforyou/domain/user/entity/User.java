package com.sparta.giftforyou.domain.user.entity;

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
    private String phoneNumber = "";

    @Column
    private Long kakaoId = 0L;

    public User(String email, String password, String nickname, String phoneNumber) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.phoneNumber = phoneNumber;
    }

    public User(String email, String password, String nickname, Long kakaoId) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.kakaoId = kakaoId;
    }

    public User kakaoIdUpdate(Long kakaoId) {
        this.kakaoId = kakaoId;
        return this;
    }
}