package com.giftforyoube.global.jwt.token.service;

import com.giftforyoube.global.exception.BaseException;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.global.jwt.constant.GrantType;
import com.giftforyoube.global.jwt.constant.TokenType;
import com.giftforyoube.global.jwt.dto.JwtTokenDto;
import io.jsonwebtoken.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;


@Slf4j
@Component
public class TokenManager {

    @Value("${spring.jwt.accsss-token-expiration-time}")
    private String accessTokenExpirationTime;
    @Value("${spring.jwt.refresh-token-expiration-time}")
    private String refreshTokenExpirationTime;
    @Value("${spring.jwt.secret.key}")
    private String tokenSecret;


    // 1. JWT 토큰 추출
    public String getTokenFromRequest(HttpServletRequest httpServletRequest) {
        String authorizationHeader = httpServletRequest.getHeader("Authorization");
        if (authorizationHeader != null) {
            return authorizationHeader.split(" ")[1];
        }

        return null;
    }

    // 2. JWT 토큰 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(tokenSecret.getBytes(StandardCharsets.UTF_8))
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException | SignatureException e) {
            throw new BaseException(BaseResponseStatus.INVALID_TOKEN);
        } catch (ExpiredJwtException e) {
            throw new BaseException(BaseResponseStatus.TOKEN_EXPIRED);
        }
    }

    // 3. JWT 토큰 내 클레임 추출
    public Claims getTokenClaims(String token) {
        Claims claims;
        try {
            claims = Jwts.parserBuilder()
                    .setSigningKey(tokenSecret.getBytes(StandardCharsets.UTF_8))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            throw new BaseException(BaseResponseStatus.INVALID_TOKEN);
        }

        return claims;
    }

    // 4. JWT 토큰 DTO 생성
    public JwtTokenDto createJwtTokenDto(String email) {
        Date AccessTokenExpireTime = createAccessTokenExpireTime();
        Date RefreshTokenExpireTime = createRefreshTokenExpireTime();

        String accessToken = createAccessToken(email, AccessTokenExpireTime);
        String refreshToken = createRefreshToken(email, RefreshTokenExpireTime);

        return JwtTokenDto.builder()
                .grantType(GrantType.BEARER.getType())
                .accessToken(accessToken)
                .accessTokenExpireTime(AccessTokenExpireTime)
                .refreshToken(refreshToken)
                .refreshTokenExpireTime(RefreshTokenExpireTime)
                .build();
    }

    // 5-1. 액세스 토큰 만료 시간 설정
    public Date createAccessTokenExpireTime() {
        return new Date(System.currentTimeMillis() + Long.parseLong(accessTokenExpirationTime));
    }

    // 5-2. 리프레시 토큰 만료 시간 설정
    public Date createRefreshTokenExpireTime() {
        return new Date(System.currentTimeMillis() + Long.parseLong(refreshTokenExpirationTime));
    }

    // 5-3. 액세스 토큰 생성
    public String createAccessToken(String email, Date expirationTime) {
        String accessToken = Jwts.builder()
                .setSubject(TokenType.ACCESS.name())
                .setIssuedAt(new Date())
                .setExpiration(expirationTime)
                .claim("email", email)
                .signWith(SignatureAlgorithm.HS512, tokenSecret.getBytes(StandardCharsets.UTF_8))
                .setHeaderParam("typ", "JWT")
                .compact();
        return accessToken;
    }

    // 5-4. 리프레시 토큰 생성
    public String createRefreshToken(String email, Date expirationTime) {
        String refreshToken = Jwts.builder()
                .setSubject(TokenType.REFRESH.name())
                .setIssuedAt(new Date())
                .setExpiration(expirationTime)
                .claim("email", email)
                .signWith(SignatureAlgorithm.HS512, tokenSecret.getBytes(StandardCharsets.UTF_8))
                .setHeaderParam("typ", "JWT")
                .compact();
        return refreshToken;
    }
}