package com.giftforyoube.user.entity;

import com.giftforyoube.donation.entity.Donation;
import com.giftforyoube.funding.entity.Funding;
import com.giftforyoube.global.common.BaseTimeEntity;
import com.giftforyoube.global.jwt.dto.JwtTokenDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.giftforyoube.global.jwt.util.DateTimeUtil.convertToLocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String email;

    @Column(nullable = false, length = 200)
    private String password;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Column(length = 20)
    private Boolean isEmailNotificationAgreed = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UserType userType;

    @Column(length = 20)
    private Long kakaoId;

    @Column(length = 50)
    private String googleId;

    @Column(length = 250)
    private String refreshToken;

    private LocalDateTime tokenExpirationTime;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Funding> fundings = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Donation> donations = new ArrayList<>();

    public User(String email, String password, String nickname, Boolean isEmailNotificationAgreed, UserType userType) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.isEmailNotificationAgreed = isEmailNotificationAgreed;
        this.userType = userType;
    }

    public User(String email, String password, String nickname, Boolean isEmailNotificationAgreed, UserType userType, Long kakaoId) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.isEmailNotificationAgreed = isEmailNotificationAgreed;
        this.userType = userType;
        this.kakaoId = kakaoId;
    }

    public User updateKakaoId(Long kakaoId) {
        this.kakaoId = kakaoId;
        return this;
    }

    public User(String email, String password, String nickname, Boolean isEmailNotificationAgreed, UserType userType, String googleId) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.isEmailNotificationAgreed = isEmailNotificationAgreed;
        this.userType = userType;
        this.googleId = googleId;
    }

    public User updateGoogleId(String googleId) {
        this.googleId = googleId;
        return this;
    }

    public void updateRefreshToken(JwtTokenDto jwtTokenDto) {
        this.refreshToken = jwtTokenDto.getRefreshToken();
        this.tokenExpirationTime = convertToLocalDateTime(jwtTokenDto.getRefreshTokenExpireTime());
    }
}