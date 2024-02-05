package com.giftforyoube.donation.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.giftforyoube.donation.dto.ReadyDonationRequestDto;
import com.giftforyoube.donation.dto.ReadyDonationResponseDto;
import com.giftforyoube.donation.service.DonationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DonationController {

    private String tid;
    private final DonationService donationService;

    public DonationController(DonationService donationService) {
        this.donationService = donationService;
    }

    @PostMapping("/donation/ready")
    public ResponseEntity readyDonation(@RequestBody ReadyDonationRequestDto readyDonationRequestDto) throws JsonProcessingException {
        ReadyDonationResponseDto readyDonationResponseDto = donationService.readyDonation(readyDonationRequestDto);
        tid = readyDonationResponseDto.getTid();
        return new ResponseEntity<>(readyDonationResponseDto, HttpStatus.OK);
    }
}