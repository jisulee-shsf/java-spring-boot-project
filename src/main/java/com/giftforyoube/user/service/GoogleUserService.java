package com.giftforyoube.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giftforyoube.global.jwt.dto.JwtTokenInfo;
import com.giftforyoube.global.jwt.util.JwtTokenUtil;
import com.giftforyoube.user.dto.OauthUserInfoDto;
import com.giftforyoube.user.entity.User;
import com.giftforyoube.user.entity.UserType;
import com.giftforyoube.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleUserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserService userService;

    @Value("${google.client.id}")
    private String clientId;
    @Value("${google.secret.password}")
    private String secretPassword;
    @Value("${google.redirect.uri}")
    private String redirectUri;

    // 구글 유저 로그인 처리
    public void googleLogin(String code, HttpServletResponse httpServletResponse) throws JsonProcessingException, UnsupportedEncodingException {
        log.info("[googleLogin] 구글 로그인 시도");

        String googleAccessToken = getGoogleAccessToken(code);
        OauthUserInfoDto.GoogleUserInfoDto googleUserInfoDto = getGoogleUserInfo(googleAccessToken);
        registerGoogleUserIfNeeded(googleUserInfoDto, httpServletResponse);
    }

    // 1. 구글 액세스 토큰 요청
    private String getGoogleAccessToken(String code) throws JsonProcessingException {
        URI uri = UriComponentsBuilder
                .fromUriString("https://oauth2.googleapis.com/token")
                .encode()
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("client_secret", secretPassword);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

        RequestEntity<MultiValueMap<String, String>> requestEntity = RequestEntity.post(uri).headers(headers).body(body);
        ResponseEntity<String> response = restTemplate.exchange(requestEntity, String.class);

        JsonNode jsonNode = new ObjectMapper().readTree(response.getBody());
        String googleAccessToken = jsonNode.get("access_token").asText();
        return googleAccessToken;
    }

    // 2. 구글 유저 정보 요청
    private OauthUserInfoDto.GoogleUserInfoDto getGoogleUserInfo(String googleAccessToken) throws JsonProcessingException {
        URI uri = UriComponentsBuilder
                .fromUriString("https://www.googleapis.com/oauth2/v2/userinfo")
                .queryParam("access_token", googleAccessToken)
                .encode()
                .build()
                .toUri();

        ResponseEntity<String> ResponseEntity = restTemplate.getForEntity(uri, String.class);
        OauthUserInfoDto.GoogleUserInfoDto googleUserInfoDto = new ObjectMapper()
                .readValue(ResponseEntity.getBody(), OauthUserInfoDto.GoogleUserInfoDto.class);
        return googleUserInfoDto;
    }

    // 3. 구글 유저 등록
    @Transactional
    public void registerGoogleUserIfNeeded(OauthUserInfoDto.GoogleUserInfoDto googleUserInfoDto,
                                           HttpServletResponse httpServletResponse) throws UnsupportedEncodingException {
        String googleId = googleUserInfoDto.getId();
        User googleUser = userRepository.findByGoogleId(googleId).orElse(null);

        if (googleUser == null) {
            String googleEmail = googleUserInfoDto.getEmail();
            User sameEmailUser = userRepository.findByEmail(googleEmail).orElse(null);
            if (sameEmailUser != null) {
                googleUser = sameEmailUser;
                googleUser = googleUser.updateGoogleId(googleId);

            } else {
                String password = UUID.randomUUID().toString();
                String encodedPassword = passwordEncoder.encode(password);
                googleUser = User.builder()
                        .email(googleUserInfoDto.getEmail())
                        .password(encodedPassword)
                        .nickname(googleUserInfoDto.getName())
                        .isEmailNotificationAgreed(false)
                        .userType(UserType.GOOGLE_USER)
                        .googleId(googleId)
                        .build();
            }
        }

        // 이메일 기반 JWT 토큰 정보 생성
        String email = googleUser.getEmail();
        JwtTokenInfo.AccessTokenInfo accessTokenInfo = jwtTokenUtil.createAccessTokenInfo(email);
        JwtTokenInfo.RefreshTokenInfo refreshTokenInfo = jwtTokenUtil.createRefreshTokenInfo(email);

        // 액세스 토큰을 쿠키에 추가해 반환
        Cookie jwtCookie = jwtTokenUtil.addTokenToCookie(accessTokenInfo.getAccessToken());
        httpServletResponse.addCookie(jwtCookie);
        log.info("[googleLogin] 쿠키 전달 완료");

        // JWT 토큰 정보 업데이트
        userService.updateAccessToken(googleUser, accessTokenInfo);
        userService.updateRefreshToken(googleUser, refreshTokenInfo);
        log.info("[googleLogin] 구글 로그인 완료");
    }
}