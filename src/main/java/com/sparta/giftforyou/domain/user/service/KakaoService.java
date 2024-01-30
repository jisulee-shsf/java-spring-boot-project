package com.sparta.giftforyou.domain.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.giftforyou.domain.user.dto.KakaoUserInfoDto;
import com.sparta.giftforyou.domain.user.entity.User;
import com.sparta.giftforyou.domain.user.repository.UserRepository;
import com.sparta.giftforyou.global.jwt.JwtUtil;
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
public class KakaoService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final JwtUtil jwtUtil;

    public KakaoService(PasswordEncoder passwordEncoder, UserRepository userRepository, RestTemplate restTemplate, JwtUtil jwtUtil) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
        this.jwtUtil = jwtUtil;
    }

    @Value("${kakao.rest.api.key}")
    private String restApiKey;
    @Value("${kakao.redirect.uri}")
    private String redirectUrl;

    public String kakaoLogin(String code) throws JsonProcessingException {
        String accessToken = getToken(code);
        KakaoUserInfoDto kakaoUserInfoDto = getKakaoUserInfo(accessToken);
        User kakaoUser = registerKakaoUserIfNeeded(kakaoUserInfoDto);
        String token = jwtUtil.createToken(kakaoUserInfoDto.getEmail());
        return token;
    }

    // access token 요청
    private String getToken(String code) throws JsonProcessingException {
        URI uri = UriComponentsBuilder
                .fromUriString("https://kauth.kakao.com")
                .path("/oauth/token")
                .encode()
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", restApiKey);
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
        String accessToken = jsonNode.get("access_token").asText();
        log.info("[Kakao | getToken] accessToken: " + accessToken);
        return accessToken;
    }

    // Kakao 사용자 정보 요청
    private KakaoUserInfoDto getKakaoUserInfo(String accessToken) throws JsonProcessingException {
        URI uri = UriComponentsBuilder
                .fromUriString("https://kapi.kakao.com")
                .path("/v2/user/me")
                .encode()
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        RequestEntity<MultiValueMap<String, String>> requestEntity = RequestEntity
                .post(uri)
                .headers(headers)
                .body(new LinkedMultiValueMap<>());

        ResponseEntity<String> response = restTemplate.exchange(
                requestEntity,
                String.class
        );

        JsonNode jsonNode = new ObjectMapper().readTree(response.getBody());
        Long kakaoId = jsonNode.get("id").asLong();
        String nickname = jsonNode.get("properties").get("nickname").asText();
        String email = jsonNode.get("kakao_account").get("email").asText();
        return new KakaoUserInfoDto(kakaoId, nickname, email);
    }

    // 조건에 따라 로그인 진행
    private User registerKakaoUserIfNeeded(KakaoUserInfoDto kakaoUserInfo) {
        Long kakaoId = kakaoUserInfo.getKakaoId();
        User kakaoUser = userRepository.findByKakaoId(kakaoId).orElse(null);

        if (kakaoUser == null) {
            String kakaoEmail = kakaoUserInfo.getEmail();
            User sameEmailUser = userRepository.findByEmail(kakaoUserInfo.getEmail()).orElse(null);
            if (sameEmailUser != null) {
                kakaoUser = sameEmailUser;
                kakaoUser = kakaoUser.kakaoIdUpdate(kakaoId);
            } else {
                String password = UUID.randomUUID().toString();
                String encodedPassword = passwordEncoder.encode(password);
                kakaoUser = new User(kakaoUserInfo.getEmail(), encodedPassword, kakaoUserInfo.getNickname(), kakaoId);
            }
            userRepository.save(kakaoUser);
        }
        return kakaoUser;
    }
}