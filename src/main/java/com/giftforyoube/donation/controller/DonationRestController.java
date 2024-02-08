package com.giftforyoube.donation.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.giftforyoube.donation.dto.ApproveDonationResponseDto;
import com.giftforyoube.donation.dto.ReadyDonationRequestDto;
import com.giftforyoube.donation.dto.ReadyDonationResponseDto;
import com.giftforyoube.donation.service.DonationService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class DonationRestController {

    private String tid;
    private final DonationService donationService;

    public DonationRestController(DonationService donationService) {
        this.donationService = donationService;
    }

    // 1. 후원 결제준비
    @PostMapping("/donation/ready")
    public ResponseEntity readyDonation(@RequestBody ReadyDonationRequestDto readyDonationRequestDto) throws JsonProcessingException {
        ReadyDonationResponseDto readyDonationResponseDto = donationService.readyDonation(readyDonationRequestDto);
        tid = readyDonationResponseDto.getTid();
        return new ResponseEntity<>(readyDonationResponseDto, HttpStatus.OK);
    }

    // 2-1. 후원 결제승인
    @GetMapping("/donation/approve")
    public void approveDonation(HttpServletResponse httpServletResponse, @RequestParam("pg_token") String pgToken) throws IOException {
        ApproveDonationResponseDto approveDonationResponseDto = donationService.approveDonation(pgToken, tid);
        httpServletResponse.sendRedirect("https://www.giftipie.me/"); // TBD
    }

    // 2-2. 후원 결제실패
    @GetMapping("/donation/fail")
    public void failDonation(HttpServletResponse httpServletResponse) throws IOException {
        httpServletResponse.sendRedirect("https://www.giftipie.me/" + "fail"); // TBD
    }

    // 2-3. 후원 결제취소
    @GetMapping("/donation/cancel")
    public void cancelDonation(HttpServletResponse httpServletResponse) throws IOException {
        httpServletResponse.sendRedirect("https://www.giftipie.me/" + "cancel"); // TBD
    }
}