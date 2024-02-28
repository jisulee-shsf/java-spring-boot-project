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
public class GoogleService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final TokenManager tokenManager;

    @Value("${google.client.id}")
    private String clientId;
    @Value("${google.secret.password}")
    private String secretPassword;
    @Value("${google.redirect.uri}")
    private String redirectUri;

    public LoginResponseDto googleLogin(String code) throws JsonProcessingException {
        log.info("[googleLogin] 구글 로그인 시도");

        String googleAccessToken = getGoogleAccessToken(code);
        OauthUserInfoDto.GoogleUserInfoDto googleUserInfoDto = getGoogleUserInfo(googleAccessToken);
        JwtTokenDto jwtTokenDto = registerGoogleUserIfNeeded(googleUserInfoDto);
        return new LoginResponseDto(jwtTokenDto);
    }

    // 1. 구글 access token 요청
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

    // 2. 구글 사용자 정보 요청
    private OauthUserInfoDto.GoogleUserInfoDto getGoogleUserInfo(String googleAccessToken) throws JsonProcessingException {
        URI uri = UriComponentsBuilder
                .fromUriString("https://www.googleapis.com/oauth2/v2/userinfo")
                .queryParam("access_token", googleAccessToken)
                .encode()
                .build()
                .toUri();

        ResponseEntity<String> ResponseEntity = restTemplate.getForEntity(uri, String.class);
        OauthUserInfoDto.GoogleUserInfoDto googleUserInfoDto = new ObjectMapper().readValue(ResponseEntity.getBody(), OauthUserInfoDto.GoogleUserInfoDto.class);
        return googleUserInfoDto;
    }

    // 3. 구글 사용자 등록
    @Transactional
    public JwtTokenDto registerGoogleUserIfNeeded(OauthUserInfoDto.GoogleUserInfoDto googleUserInfoDto) {
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
                googleUser = new User(googleUserInfoDto.getEmail(), encodedPassword, googleUserInfoDto.getName(), false, UserType.GOOGLE_USER, googleId);
            }
        }

        JwtTokenDto jwtTokenDto = tokenManager.createJwtTokenDto(googleUser.getEmail());
        googleUser.updateRefreshToken(jwtTokenDto);
        userRepository.save(googleUser);
        log.info("[googleLogin] 구글 로그인 완료");
        return jwtTokenDto;
    }
}