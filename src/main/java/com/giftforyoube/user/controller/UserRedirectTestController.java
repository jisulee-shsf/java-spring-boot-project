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
public class UserRedirectTestController {

    @GetMapping("/test/uri")
    public URI redirectToLoginSuccessUri() throws URISyntaxException {
        return new URI("https://www.giftipie.me/");
    }

    @GetMapping("/test/location")
    public ResponseEntity<Void> redirectToLoginSuccessLocation() throws URISyntaxException {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(new URI("https://www.giftipie.me/"));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}