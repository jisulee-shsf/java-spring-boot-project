package com.giftforyoube.global.jwt.util;

import com.giftforyoube.global.exception.BaseException;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.global.jwt.constant.GrantType;
import com.giftforyoube.global.jwt.constant.TokenType;
import com.giftforyoube.global.jwt.dto.JwtTokenInfo;
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
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenUtil {

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

    // 1. JWT 토큰 추출(쿠키)
    public String getTokenFromRequest(HttpServletRequest httpServletRequest) {
        Cookie[] cookies = httpServletRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(AUTHORIZATION_HEADER)) {
                    try {
                        String decodedToken = URLDecoder.decode(cookie.getValue(), "UTF-8");
                        return decodedToken;
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
            if (token.startsWith(GrantType.BEARER.getType())) {
                String tokenSubstring = token.substring(7);
                return tokenSubstring;
            } else {
                throw new BaseException(BaseResponseStatus.INVALID_BEARER_GRANT_TYPE);
            }
        }
        throw new BaseException(BaseResponseStatus.TOKEN_NOT_FOUND);
    }

    // 3. JWT 토큰 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            log.info("[validateToken] 검증 완료");
            return true;
        } catch (SecurityException | MalformedJwtException | SignatureException e) {
            throw new BaseException(BaseResponseStatus.TOKEN_INVALID);
        } catch (ExpiredJwtException e) {
            throw new BaseException(BaseResponseStatus.TOKEN_EXPIRED);
        }
    }

    // 4. JWT 토큰 내 클레임 추출
    public Claims getTokenClaims(String token) {
        Claims claims;
        try {
            claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            throw new BaseException(BaseResponseStatus.TOKEN_INVALID);
        }
        log.info("[getTokenClaims] 클레임 추출");
        return claims;
    }

    // 5-1. 액세스 토큰 만료 시간 설정
    public Date createAccessTokenExpirationTime() {
        return new Date(System.currentTimeMillis() + Long.parseLong(accessTokenExpirationTime));
    }

    // 5-2. 리프레시 토큰 만료 시간 설정
    public Date createRefreshTokenExpirationTime() {
        return new Date(System.currentTimeMillis() + Long.parseLong(refreshTokenExpirationTime));
    }

    // 6-1. 액세스 토큰 생성
    public String createAccessToken(String email, Date expirationTime) {
        String accessToken = Jwts.builder()
                .setSubject(TokenType.ACCESS.name())
                .setIssuedAt(new Date())
                .setExpiration(expirationTime)
                .claim("email", email)
                .signWith(key, signatureAlgorithm)
                .setHeaderParam("typ", "JWT")
                .compact();
        return accessToken;
    }

    // 6-2. 리프레시 토큰 생성
    public String createRefreshToken(String email, Date expirationTime) {
        String refreshToken = Jwts.builder()
                .setSubject(TokenType.REFRESH.name())
                .setIssuedAt(new Date())
                .setExpiration(expirationTime)
                .claim("email", email)
                .signWith(key, signatureAlgorithm)
                .setHeaderParam("typ", "JWT")
                .compact();
        return refreshToken;
    }

    // 7-1. 액세스 토큰 정보 생성
    public JwtTokenInfo.AccessTokenInfo createAccessTokenInfo(String email) {
        String accessToken = createAccessToken(email, createAccessTokenExpirationTime());
        log.info("[createAccessTokenInfo] 액세스 토큰 발급");
        return JwtTokenInfo.AccessTokenInfo.builder()
                .grantType(GrantType.BEARER.getType())
                .accessToken(accessToken)
                .accessTokenExpireTime(createAccessTokenExpirationTime())
                .build();
    }

    // 7-2. 리프레시 토큰 정보 생성
    public JwtTokenInfo.RefreshTokenInfo createRefreshTokenInfo(String email) {
        String refreshToken = createRefreshToken(email, createRefreshTokenExpirationTime());
        log.info("[createAccessTokenInfo] 리프레시 토큰 발급");
        return JwtTokenInfo.RefreshTokenInfo.builder()
                .grantType(GrantType.BEARER.getType())
                .refreshToken(refreshToken)
                .refreshTokenExpireTime(createRefreshTokenExpirationTime())
                .build();
    }

    // 8. 쿠키 내 JWT 토큰 추가
    public Cookie addTokenToCookie(String token) throws UnsupportedEncodingException {
        String encodedToken = URLEncoder.encode(BEARER_PREFIX + token, "utf-8").replaceAll("\\+", "%20");
        Cookie cookie = new Cookie(AUTHORIZATION_HEADER, encodedToken);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        log.info("[addTokenToCookie] 쿠키 내 JWT 토큰 추가");
        return cookie;
    }

    // 9. 쿠키 내 JWT 토큰 삭제
    public Cookie removeTokenCookie() {
        Cookie cookie = new Cookie(AUTHORIZATION_HEADER, "");
        cookie.setValue("");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        log.info("[removeTokenCookie] 쿠키 내 JWT 토큰 삭제");
        return cookie;
    }
}