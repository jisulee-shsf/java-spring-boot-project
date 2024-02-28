package com.giftforyoube.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giftforyoube.global.jwt.dto.JwtTokenDto;
import com.giftforyoube.global.jwt.token.service.TokenManager;
import com.giftforyoube.user.dto.LoginResponseDto;
import com.giftforyoube.user.dto.OauthUserInfoDto;
import com.giftforyoube.user.entity.User;
import com.giftforyoube.user.entity.UserType;
import com.giftforyoube.user.repository.UserRepository;
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

import java.net.URI;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final TokenManager tokenManager;

    @Value("${kakao.rest.api.key}")
    private String restApiKey;
    @Value("${kakao.redirect.uri}")
    private String redirectUri;

    public LoginResponseDto kakaoLogin(String code) throws JsonProcessingException {
        log.info("[kakaoLogin] 카카오 로그인 시도");

        String kakaoAccessToken = getKakaoAccessToken(code);
        OauthUserInfoDto.KakaoUserInfoDto kakaoUserInfoDto = getKakaoUserInfo(kakaoAccessToken);
        JwtTokenDto jwtTokenDto = registerKakaoUserIfNeeded(kakaoUserInfoDto);
        return new LoginResponseDto(jwtTokenDto);
    }

    // 1. 카카오 access token 요청
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
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

        RequestEntity<MultiValueMap<String, String>> requestEntity = RequestEntity.post(uri).headers(headers).body(body);
        ResponseEntity<String> responseEntity = restTemplate.exchange(requestEntity, String.class);

        JsonNode jsonNode = new ObjectMapper().readTree(responseEntity.getBody());
        String kakaoAccessToken = jsonNode.get("access_token").asText();
        return kakaoAccessToken;
    }

    // 2. 카카오 사용자 정보 요청
    private OauthUserInfoDto.KakaoUserInfoDto getKakaoUserInfo(String kakaoAccessToken) throws JsonProcessingException {
        URI uri = UriComponentsBuilder
                .fromUriString("https://kapi.kakao.com")
                .path("/v2/user/me")
                .encode()
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + kakaoAccessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        RequestEntity<MultiValueMap<String, String>> requestEntity = RequestEntity.post(uri).headers(headers).body(new LinkedMultiValueMap<>());
        ResponseEntity<String> response = restTemplate.exchange(requestEntity, String.class);

        JsonNode jsonNode = new ObjectMapper().readTree(response.getBody());
        Long kakaoId = jsonNode.get("id").asLong();
        String nickname = jsonNode.get("properties").get("nickname").asText();
        String email = jsonNode.get("kakao_account").get("email").asText();
        OauthUserInfoDto.KakaoUserInfoDto kakaoUserInfoDto = new OauthUserInfoDto.KakaoUserInfoDto(kakaoId, nickname, email);
        return kakaoUserInfoDto;
    }

    // 3. 카카오 사용자 등록
    @Transactional
    public JwtTokenDto registerKakaoUserIfNeeded(OauthUserInfoDto.KakaoUserInfoDto kakaoUserInfoDto) {
        Long kakaoId = kakaoUserInfoDto.getId();
        User kakaoUser = userRepository.findByKakaoId(kakaoId).orElse(null);

        if (kakaoUser == null) {
            String kakaoEmail = kakaoUserInfoDto.getEmail();
            User sameEmailUser = userRepository.findByEmail(kakaoEmail).orElse(null);
            if (sameEmailUser != null) {
                kakaoUser = sameEmailUser;
                kakaoUser = kakaoUser.updateKakaoId(kakaoId);
            } else {
                String password = UUID.randomUUID().toString();
                String encodedPassword = passwordEncoder.encode(password);
                kakaoUser = new User(kakaoUserInfoDto.getEmail(), encodedPassword, kakaoUserInfoDto.getNickname(), false, UserType.KAKAO_USER, kakaoId);
            }
        }

        JwtTokenDto jwtTokenDto = tokenManager.createJwtTokenDto(kakaoUser.getEmail());
        kakaoUser.updateRefreshToken(jwtTokenDto);
        userRepository.save(kakaoUser);
        log.info("[kakaoLogin] 카카오 로그인 완료");
        return jwtTokenDto;
    }
}