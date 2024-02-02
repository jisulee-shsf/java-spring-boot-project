package com.giftforyoube.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giftforyoube.global.jwt.JwtUtil;
import com.giftforyoube.user.dto.KakaoUserInfoDto;
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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.UUID;

@Slf4j
@Service
public class KakaoService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final JwtUtil jwtUtil;

    @Getter
    private String kakaoAccessToken;

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

    // 카카오 로그인
    public String kakaoLogin(String code) throws JsonProcessingException, UnsupportedEncodingException {
        String kakaoAccessToken = getKakaoAccessToken(code);
        KakaoUserInfoDto kakaoUserInfoDto = getKakaoUserInfo(kakaoAccessToken);
        User kakaoUser = registerKakaoUserIfNeeded(kakaoUserInfoDto);
        String kakaoToken = jwtUtil.createToken(kakaoUser.getEmail());
        kakaoToken = URLEncoder.encode(kakaoToken, "UTF-8").replaceAll("\\+", "%20");
        return kakaoToken;
    }

    // 1. access token 요청
    private String getKakaoAccessToken(String code) throws JsonProcessingException {
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
        String kakaoAccessToken = jsonNode.get("access_token").asText();
        log.info("[Kakao | getAccessToken] " + kakaoAccessToken);
        this.kakaoAccessToken = kakaoAccessToken;
        return kakaoAccessToken;
    }

    // 2. Kakao 사용자 정보 요청
    private KakaoUserInfoDto getKakaoUserInfo(String kakaoAccessToken) throws JsonProcessingException {
        URI uri = UriComponentsBuilder
                .fromUriString("https://kapi.kakao.com")
                .path("/v2/user/me")
                .encode()
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + kakaoAccessToken);
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

    // 3. 조건에 따라 Kakao 로그인 진행
    private User registerKakaoUserIfNeeded(KakaoUserInfoDto kakaoUserInfoDto) {
        Long kakaoId = kakaoUserInfoDto.getId();
        log.info("[Kakao | registerKakaoUserIfNeeded] kakaoId: " + kakaoId);
        User kakaoUser = userRepository.findByKakaoId(kakaoId).orElse(null);

        if (kakaoUser == null) {
            String kakaoEmail = kakaoUserInfoDto.getEmail();
            User sameEmailUser = userRepository.findByEmail(kakaoEmail).orElse(null);
            if (sameEmailUser != null) {
                kakaoUser = sameEmailUser;
                kakaoUser = kakaoUser.kakaoIdAndAccessTokenUpdate(kakaoId, kakaoAccessToken);
            } else {
                String password = UUID.randomUUID().toString();
                String encodedPassword = passwordEncoder.encode(password);
                kakaoUser = new User(kakaoUserInfoDto.getEmail(), encodedPassword, kakaoUserInfoDto.getNickname(), kakaoId, kakaoAccessToken, null);
            }
        }
        kakaoUser = kakaoUser.kakaoAccessTokenUpdate(kakaoAccessToken);
        userRepository.save(kakaoUser);
        return kakaoUser;
    }
}