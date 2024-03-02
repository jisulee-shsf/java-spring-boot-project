package com.giftforyoube.global.jwt.token.service;

import com.giftforyoube.global.exception.BaseException;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.global.jwt.constant.GrantType;
import com.giftforyoube.global.jwt.constant.TokenType;
import com.giftforyoube.global.jwt.dto.JwtTokenDto;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.Key;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;

import static com.giftforyoube.global.jwt.util.DateTimeUtil.convertToLocalDateTime;

@Slf4j
@Component
public class TokenManager {

    @Value("${spring.jwt.accsss-token-expiration-time}")
    private String accessTokenExpirationTime;
    @Value("${spring.jwt.refresh-token-expiration-time}")
    private String refreshTokenExpirationTime;
    @Value("${spring.jwt.secret.key}")
    private String tokenSecret;

    private Key key;
    private final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    @PostConstruct
    public void init() {
        byte[] bytes = Base64.getDecoder().decode(tokenSecret);
        key = Keys.hmacShaKeyFor(bytes);
    }

    // 1. 쿠키로 부터 JWT 토큰 추출
    public String getTokenFromRequest(HttpServletRequest httpServletRequest) {
        Cookie[] cookies = httpServletRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(AUTHORIZATION_HEADER)) {
                    try {
                        String token = URLDecoder.decode(cookie.getValue(), "UTF-8");
                        log.info("[getTokenFromRequest] token: " + token);
                        return token;
                    } catch (UnsupportedEncodingException e) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    // 2. JWT 토큰 내 Bearer 타입 제거
    public static String substringToken(String token) {
        if (StringUtils.hasText(token)) {
            if (token.startsWith(BEARER_PREFIX)) {
                String tokenValue = token.substring(7);
                log.info("[substringToken] tokenValue: " + tokenValue);
                return tokenValue;
            } else {
                throw new BaseException(BaseResponseStatus.INVALID_BEARER_GRANT_TYPE);
            }
        }
        throw new BaseException(BaseResponseStatus.ACCESS_TOKEN_NOT_FOUND);
    }

    // 3. JWT 토큰 검증
    public boolean validateToken(String tokenValue) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(tokenValue);
            return true;
        } catch (SecurityException | MalformedJwtException | SignatureException e) {
            throw new BaseException(BaseResponseStatus.ACCESS_TOKEN_INVALID);
        } catch (ExpiredJwtException e) {
            throw new BaseException(BaseResponseStatus.ACCESS_TOKEN_EXPIRED);
        }
    }

    // 4. JWT 토큰 내 Claims 추출
    public Claims getTokenClaims(String tokenValue) {
        Claims claims;
        try {
            claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(tokenValue)
                    .getBody();
        } catch (Exception e) {
            throw new BaseException(BaseResponseStatus.ACCESS_TOKEN_INVALID);
        }
        return claims;
    }

    public boolean isRefreshTokenValid(String tokenValue) {
        Claims claims = getTokenClaims(tokenValue);
        Date expiration = claims.getExpiration();
        LocalDateTime tokenExpirationTime = convertToLocalDateTime(expiration);

        return tokenExpirationTime != null && !tokenExpirationTime.isBefore(LocalDateTime.now());
    }

    // 5. JWT 토큰 DTO 생성
    public JwtTokenDto createJwtTokenDto(String email) {
        Date accessTokenExpireTime = createAccessTokenExpireTime();
        Date refreshTokenExpireTime = createRefreshTokenExpireTime();

        String accessToken = createAccessToken(email, accessTokenExpireTime);
        String refreshToken = createRefreshToken(email, refreshTokenExpireTime);

        return JwtTokenDto.builder()
                .grantType(GrantType.BEARER.getType())
                .accessToken(accessToken)
                .accessTokenExpireTime(accessTokenExpireTime)
                .refreshToken(refreshToken)
                .refreshTokenExpireTime(refreshTokenExpireTime)
                .build();
    }

    // 6-1. 액세스 토큰 만료 시간 설정
    public Date createAccessTokenExpireTime() {
        return new Date(System.currentTimeMillis() + Long.parseLong(accessTokenExpirationTime));
    }

    // 6-2. 리프레시 토큰 만료 시간 설정
    public Date createRefreshTokenExpireTime() {
        return new Date(System.currentTimeMillis() + Long.parseLong(refreshTokenExpirationTime));
    }

    // 7-1. 액세스 토큰 생성
    public String createAccessToken(String email, Date expirationTime) {
        String accessToken = BEARER_PREFIX + Jwts.builder()
                .setSubject(TokenType.ACCESS.name())
                .setIssuedAt(new Date())
                .setExpiration(expirationTime)
                .claim("email", email)
                .signWith(key, signatureAlgorithm)
                .compact();
        log.info("[createAccessToken] accessToken: " + accessToken);
        return accessToken;
    }

    // 7-2. 리프레시 토큰 생성
    public String createRefreshToken(String email, Date expirationTime) {
        String refreshToken = Jwts.builder()
                .setSubject(TokenType.REFRESH.name())
                .setIssuedAt(new Date())
                .setExpiration(expirationTime)
                .claim("email", email)
                .signWith(key, signatureAlgorithm)
                .setHeaderParam("typ", "JWT")
                .compact();
        log.info("[createRefreshToken] refreshToken: " + refreshToken);
        return refreshToken;
    }

    // 7-3. 액세스 토큰 쿠키 추가
    public Cookie addTokenToCookie(String accessToken) throws UnsupportedEncodingException {
        String encodeToken = URLEncoder.encode(accessToken, "utf-8").replaceAll("\\+", "%20");
        Cookie cookie = new Cookie(AUTHORIZATION_HEADER, encodeToken);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        return cookie;
    }
}