package com.sparta.giftforyou.global.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.Key;
import java.util.Base64;
import java.util.Date;


@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret.key}")
    private String secretKey; // Base64 encode 처리된 secret key
    private Key key; // secret key를 담을 객체
    private final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256; // 암호화 알고리즘
    public static final String AUTHORIZATION_HEADER = "Authorization"; // cookie name 또는 header key 값
    public static final String BEARER_PREFIX = "Bearer "; // token 식별자
    private final long TOKEN_TIME = 24 * 60 * 60 * 1000L; // token 만료 시간

    @PostConstruct
    public void init() {
        byte[] bytes = Base64.getDecoder().decode(secretKey); // secret key Base64 decode 처리
        key = Keys.hmacShaKeyFor(bytes);
    }

    public String createToken(String email) {
        Date date = new Date();
        String token = BEARER_PREFIX +
                Jwts.builder()
                        .setSubject(email) // 사용자 식별값 설정
                        .setExpiration(new Date(date.getTime() + TOKEN_TIME)) // 만료 시간 설정
                        .setIssuedAt(date) // 발급 일자 설정
                        .signWith(key, signatureAlgorithm) // 암호화 알고리즘 설정
                        .compact();
        log.info("[createToken] token: " + token);
        return token;
    }

    public static String addJwtToCookie(String token, HttpServletResponse res) {
//            token = URLEncoder.encode(token, "utf-8").replaceAll("\\+", "%20"); // BEARER_PREFIX 공백 대체
        String valueToken = substringToken(token);
        log.info("[addJwtToCookie] valueToken: " + valueToken);
        Cookie cookie = new Cookie(AUTHORIZATION_HEADER, valueToken); // JWT 포함 cookie 생성
        cookie.setPath("/"); // cookie 경로 설정
        res.addCookie(cookie); // HttpServletResponse 객체 내 cookie 추가
        return valueToken;
    }

    public static String substringToken(String token) {
        log.info("[before substringToken] token: " + token);
        if (StringUtils.hasText(token) && token.startsWith(BEARER_PREFIX)) { // 공백 또는 null 여부 & BEARER_PREFIX로 시작 여부 확인
            String tokenValue = token.substring(7); // BEARER_PREFIX 제거
            log.info("[after substringToken] tokenValue: " + tokenValue);
            return tokenValue;
        }
        log.error("Not Found Token");
        throw new NullPointerException("Not Found Token");
    }

    public boolean validateToken(String tokenValue) {
        log.info("[before validateToken] tokenValue: " + tokenValue);
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(tokenValue);
            return true;
        } catch (SecurityException | MalformedJwtException | SignatureException e) {
            log.error("Invalid JWT signature");
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException e) {
            log.error("JWT claims is empty");
        }
        return false;
    }

    public Claims getUserInfoFromToken(String tokenValue) {

        log.info("[getUserInfoFromToken] tokenValue: " + tokenValue);
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(tokenValue).getBody();
    }

    public String getTokenFromRequest(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(AUTHORIZATION_HEADER)) {
                    try {
                        return URLDecoder.decode(cookie.getValue(), "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        return null;
                    }
                }
            }
        }
        return null;
    }
}