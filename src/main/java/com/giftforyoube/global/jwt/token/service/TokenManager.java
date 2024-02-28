package com.giftforyoube.global.jwt.token.service;

import com.giftforyoube.global.exception.BaseException;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.global.jwt.constant.GrantType;
import com.giftforyoube.global.jwt.constant.TokenType;
import com.giftforyoube.global.jwt.dto.JwtTokenDto;
import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@RequiredArgsConstructor
public class TokenManager {

    private final String accessTokenExpirationTime;
    private final String refreshTokenExpirationTime;
    private final String tokenSecret;

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

    public Date createAccessTokenExpireTime() {
        return new Date(System.currentTimeMillis() + Long.parseLong(accessTokenExpirationTime));
    }

    public Date createRefreshTokenExpireTime() {
        return new Date(System.currentTimeMillis() + Long.parseLong(refreshTokenExpirationTime));
    }

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

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(tokenSecret.getBytes(StandardCharsets.UTF_8))
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException | SignatureException e) {
            throw new BaseException(BaseResponseStatus.NOT_VALID_TOKEN);
        } catch (ExpiredJwtException e) {
            throw new BaseException(BaseResponseStatus.TOKEN_EXPIRED);
        }
    }

    public Claims getTokenClaims(String token) {
        Claims claims;
        try {
            claims = Jwts.parserBuilder()
                    .setSigningKey(tokenSecret.getBytes(StandardCharsets.UTF_8))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            throw new BaseException(BaseResponseStatus.NOT_VALID_TOKEN);
        }
        return claims;
    }
}