package com.giftforyoube.user.entity;

import com.giftforyoube.funding.entity.Funding;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    @Column(unique = true)
    private String phoneNumber;

    // User 엔티티를 저장할 때 자동으로 연결된 Funding 엔티티도 저장되도록 cascade = CascadeType.ALL
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Funding> fundings = new ArrayList<>();

    @Column
    private Long kakaoId;

    @Column
    private String kakaoAccessToken;

    @Column
    private String googleId;

    @Column
    private String googleAccessToken;

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

    public User(String email, String password, String nickname, String googleId, String googleAccessToken, String phoneNumber) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.googleId = googleId;
        this.googleAccessToken = googleAccessToken;
        this.phoneNumber = phoneNumber;
    }

    public User googleIdAndAccessTokenUpdate(String googleId, String googleAccessToken) {
        this.googleId = googleId;
        this.googleAccessToken = googleAccessToken;
        return this;
    }

    public User googleAccessTokenUpdate(String googleAccessToken) {
        this.googleAccessToken = googleAccessToken;
        return this;
    }
}