package com.giftforyoube.user.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URISyntaxException;

@RestController
@RequestMapping("/api/login")
public class UserRedirectController {

    // 유저 테스트 시 적용 예정
    @GetMapping("/success")
    public ResponseEntity<Void> redirectToLoginSuccessUri() throws URISyntaxException {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(new URI("https://www.giftipie.me/"));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    // 유저 테스트 시 적용 예정
    @GetMapping("/fail")
    public ResponseEntity<Void> redirectToLoginFailUri() throws URISyntaxException {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(new URI("https://www.giftipie.me/"));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}