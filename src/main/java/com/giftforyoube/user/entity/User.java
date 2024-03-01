package com.giftforyoube.user.entity;

import com.giftforyoube.donation.entity.Donation;
import com.giftforyoube.funding.entity.Funding;
import com.giftforyoube.global.common.BaseTimeEntity;
import com.giftforyoube.global.jwt.dto.JwtTokenDto;
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
    private String refreshToken;

    private LocalDateTime tokenExpirationTime;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Funding> fundings = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Donation> donations = new ArrayList<>();

    @Builder
    public User(Long id, String email, String password, String nickname, Boolean isEmailNotificationAgreed,
                UserType userType, Long kakaoId, String googleId, String refreshToken,
                LocalDateTime tokenExpirationTime, List<Funding> fundings, List<Donation> donations) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.isEmailNotificationAgreed = isEmailNotificationAgreed;
        this.userType = userType;
        this.kakaoId = kakaoId;
        this.googleId = googleId;
        this.refreshToken = refreshToken;
        this.tokenExpirationTime = tokenExpirationTime;
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

    public void updateRefreshToken(JwtTokenDto jwtTokenDto) {
        this.refreshToken = jwtTokenDto.getRefreshToken();
        this.tokenExpirationTime = convertToLocalDateTime(jwtTokenDto.getRefreshTokenExpireTime());
    }

    public void expireRefreshToken(LocalDateTime now) {
        this.tokenExpirationTime = now;
    }
}