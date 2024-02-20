package com.giftforyoube.home.controller;

import com.giftforyoube.funding.dto.FundingResponseDto;
import com.giftforyoube.funding.service.FundingService;
import com.giftforyoube.global.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class HomeController {
    private final FundingService fundingService;

    // 메인페이지에 보여질 내 펀딩 정보
    @GetMapping
    public ResponseEntity<FundingResponseDto> getMyFundingInfo(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return new ResponseEntity<>(fundingService.getMyFundingInfo(userDetails.getUser()), HttpStatus.OK);
    }
}
