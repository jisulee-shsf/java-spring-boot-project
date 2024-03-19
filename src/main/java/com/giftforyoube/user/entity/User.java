package com.giftforyoube.user.entity;

import com.giftforyoube.donation.entity.Donation;
import com.giftforyoube.funding.entity.Funding;
import com.giftforyoube.global.common.BaseTimeEntity;
import com.giftforyoube.global.jwt.dto.JwtTokenInfo;
import jakarta.persistence.*;
import lombok.Builder;
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
    private String accessToken;

    private LocalDateTime accessTokenExpirationTime;

    @Column(length = 250)
    private String refreshToken;

    private LocalDateTime refreshTokenExpirationTime;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Funding> fundings = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Donation> donations = new ArrayList<>();

    @Builder
    public User(Long id, String email, String password, String nickname, Boolean isEmailNotificationAgreed,
                UserType userType, Long kakaoId, String googleId, String accessToken,
                LocalDateTime accessTokenExpirationTime, String refreshToken,
                LocalDateTime refreshTokenExpirationTime, List<Funding> fundings, List<Donation> donations) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.isEmailNotificationAgreed = isEmailNotificationAgreed;
        this.userType = userType;
        this.kakaoId = kakaoId;
        this.googleId = googleId;
        this.accessToken = accessToken;
        this.accessTokenExpirationTime = accessTokenExpirationTime;
        this.refreshToken = refreshToken;
        this.refreshTokenExpirationTime = refreshTokenExpirationTime;
        this.fundings = fundings;
        this.donations = donations;
    }

    public User updateKakaoId(Long kakaoId) {
        this.kakaoId = kakaoId;
        return this;
    }

    public User updateGoogleId(String googleId) {
        this.googleId = googleId;
        return this;
    }

    public void updateRefreshTokenInfo(JwtTokenInfo.RefreshTokenInfo refreshTokenInfo) {
        this.refreshToken = refreshTokenInfo.getRefreshToken();
        this.refreshTokenExpirationTime = convertToLocalDateTime(refreshTokenInfo.getRefreshTokenExpireTime());
    }

    public void updateAccessTokenInfo(JwtTokenInfo.AccessTokenInfo accessTokenInfo) {
        this.accessToken = accessTokenInfo.getAccessToken();
        this.accessTokenExpirationTime = convertToLocalDateTime(accessTokenInfo.getAccessTokenExpireTime());
    }

    public void expireRefreshTokenExpirationTime(LocalDateTime now) {
        this.refreshTokenExpirationTime = now;
    }

    public void expireAccessTokenExpirationTime(LocalDateTime now) {
        this.accessTokenExpirationTime = now;
    }

    // 테스트코드용 //
    public void setId(Long id) {
        this.id = id;
    }
}