package com.giftforyoube.user.entity;

import com.giftforyoube.donation.entity.Donation;
import com.giftforyoube.funding.entity.Funding;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@Component
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

    @Column
    private Boolean isEmailNotificationAgreed = false;

    // User 엔티티를 저장할 때 자동으로 연결된 Funding 엔티티도 저장되도록 cascade = CascadeType.ALL
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Funding> fundings = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Donation> donations = new ArrayList<>();

    @Column
    private Long kakaoId;

    @Column
    private String kakaoAccessToken;

    @Column
    private String googleId;

    @Column
    private String googleAccessToken;

    // 1. 일반 회원가입
    public User(String email, String password, String nickname, Boolean isEmailNotificationAgreed) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.isEmailNotificationAgreed = isEmailNotificationAgreed;
    }

    // 2-1. 카카오 로그인
    public User(String email, String password, String nickname, Long kakaoId, String kakaoAccessToken, Boolean isEmailNotificationAgreed) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.kakaoId = kakaoId;
        this.kakaoAccessToken = kakaoAccessToken;
        this.isEmailNotificationAgreed = isEmailNotificationAgreed;
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

    // 2-2. 구글 로그인
    public User(String email, String password, String nickname, String googleId, String googleAccessToken, Boolean isEmailNotificationAgreed) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.googleId = googleId;
        this.googleAccessToken = googleAccessToken;
        this.isEmailNotificationAgreed = isEmailNotificationAgreed;
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

    // 테스트코드용 //
    public void setId(Long id) {
        this.id = id;
    }
}