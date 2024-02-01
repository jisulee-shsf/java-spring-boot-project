package com.giftforyoube.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giftforyoube.global.jwt.JwtUtil;
import com.giftforyoube.user.dto.GoogleUserInfoDto;
import com.giftforyoube.user.entity.User;
import com.giftforyoube.user.repository.UserRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@Slf4j
@Service
public class GoogleService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final JwtUtil jwtUtil;

    @Getter
    private String googleAccessToken;

    public GoogleService(PasswordEncoder passwordEncoder, UserRepository userRepository, RestTemplate restTemplate, JwtUtil jwtUtil) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
        this.jwtUtil = jwtUtil;
    }

    @Value("${google.client.id}")
    private String clientId;
    @Value("${google.secret.password}")
    private String secretPassword;
    @Value("${google.redirect.uri}")
    private String redirectUrl;

    public String googleLogin(String code) throws JsonProcessingException {
        String googleAccessToken = getGoogleAccessToken(code);
        GoogleUserInfoDto googleUserInfoDto = getGoogleUserInfo(googleAccessToken);
        User googleUser = registerGoogleUserIfNeeded(googleUserInfoDto);
        String googleToken = jwtUtil.createToken(googleUser.getEmail());
        log.info("[Google | googleToken] " + googleToken);
        return googleToken;
    }

    // access token 요청
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
        body.add("redirect_uri", redirectUrl);
        body.add("code", code);

        RequestEntity<MultiValueMap<String, String>> requestEntity = RequestEntity
                .post(uri)
                .headers(headers)
                .body(body);

        ResponseEntity<String> response = restTemplate.exchange(
                requestEntity,
                String.class
        );

        JsonNode jsonNode = new ObjectMapper().readTree(response.getBody());
        String googleAccessToken = jsonNode.get("access_token").asText();
        log.info("[Google | getAccessToken] " + googleAccessToken);
        this.googleAccessToken = googleAccessToken;
        return googleAccessToken;
    }

    // Google 사용자 정보 요청
    private GoogleUserInfoDto getGoogleUserInfo(String googleAccessToken) throws JsonProcessingException {
        URI uri = UriComponentsBuilder
                .fromUriString("https://www.googleapis.com/oauth2/v2/userinfo")
                .queryParam("access_token", googleAccessToken)
                .encode()
                .build()
                .toUri();

        ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);

        GoogleUserInfoDto googleUserInfoDto = new ObjectMapper().readValue(response.getBody(), GoogleUserInfoDto.class);
        log.info("test: " + googleUserInfoDto.getId());
        return googleUserInfoDto;
    }

    // 조건에 따라 로그인 진행
    private User registerGoogleUserIfNeeded(GoogleUserInfoDto googleUserInfoDto) {
        log.info("[Google | registerGoogleUserIfNeeded.sth] " + googleUserInfoDto.getEmail());
        String googleId = googleUserInfoDto.getId();
        log.info("[Google | registerGoogleUserIfNeeded] googleId: " + googleId);
        User googleUser = userRepository.findByGoogleId(googleId).orElse(null);
        log.info("[Google | registerGoogleUserIfNeeded] googleUser: " + googleUser);

        if (googleUser == null) {
            String googleEmail = googleUserInfoDto.getEmail();
            User sameEmailUser = userRepository.findByEmail(googleEmail).orElse(null);
            if (sameEmailUser != null) {
                googleUser = sameEmailUser;
                googleUser = googleUser.googleIdAndAccessTokenUpdate(googleId, googleAccessToken);
            } else {
                String password = UUID.randomUUID().toString();
                String encodedPassword = passwordEncoder.encode(password);
                googleUser = new User(googleUserInfoDto.getEmail(), encodedPassword, googleUserInfoDto.getName(), googleId, googleAccessToken, null);
            }
        }
        googleUser = googleUser.googleAccessTokenUpdate(googleAccessToken);
        userRepository.save(googleUser);
        return googleUser;
    }
}