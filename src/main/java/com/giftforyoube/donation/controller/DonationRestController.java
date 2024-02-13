package com.giftforyoube.donation.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.giftforyoube.donation.dto.DonationInfoDto;
import com.giftforyoube.donation.dto.ReadyDonationRequestDto;
import com.giftforyoube.donation.dto.ReadyDonationResponseDto;
import com.giftforyoube.donation.service.DonationService;
import com.giftforyoube.global.exception.BaseResponse;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.global.security.UserDetailsImpl;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

@RestController
@RequestMapping("/api")
public class DonationRestController {

    private final DonationService donationService;
    private final HttpSession session;

    public DonationRestController(DonationService donationService, HttpSession session) {
        this.donationService = donationService;
        this.session = session;
    }

    // 1. 후원 준비
    @PostMapping("/funding/{fundingId}/donation/ready")
    public ResponseEntity<ReadyDonationResponseDto> readyDonation(@PathVariable Long fundingId,
                                                                  @RequestBody ReadyDonationRequestDto readyDonationRequestDto) throws JsonProcessingException {
        ReadyDonationResponseDto readyDonationResponseDto = donationService.readyDonation(readyDonationRequestDto);
        session.setAttribute("fundingId", fundingId); // TEST
        session.setAttribute("sponsorNickname", readyDonationRequestDto.getSponsorNickname());
        session.setAttribute("comment", readyDonationRequestDto.getComment());
        session.setAttribute("tid", readyDonationResponseDto.getTid());
        return new ResponseEntity<>(readyDonationResponseDto, HttpStatus.OK);
    }

    // 2-1. 후원 승인
    @GetMapping("/donation/approve")
    public ResponseEntity<BaseResponse<DonationInfoDto>> approveDonation(@RequestParam("pg_token") String pgToken,
                                                                         @AuthenticationPrincipal UserDetailsImpl userDetails) throws JsonProcessingException, MalformedURLException, URISyntaxException {
        String tid = (String) session.getAttribute("tid");
        String sponsorNickname = (String) session.getAttribute("sponsorNickname");
        String comment = (String) session.getAttribute("comment");
        Long fundingId = (Long) session.getAttribute("fundingId");
        DonationInfoDto donationInfoDto = donationService.approveDonation(tid, pgToken, sponsorNickname, comment, fundingId, userDetails);

        BaseResponse<DonationInfoDto> baseResponse = new BaseResponse<>(BaseResponseStatus.DONATION_SUCCESS, donationInfoDto);
        return ResponseEntity.status(HttpStatus.FOUND) // 302
                .location(new URL("https://www.giftipie.me/").toURI())
                .body(baseResponse); // 2000
    }

    // 2-2. 후원 실패
    @GetMapping("/donation/fail")
    public void failDonation(HttpServletResponse httpServletResponse) throws IOException {
        httpServletResponse.sendRedirect("https://www.giftipie.me/donation/fail"); // TBD
    }

    // 2-3. 후원 취소
    @GetMapping("/donation/cancel")
    public void cancelDonation(HttpServletResponse httpServletResponse) throws IOException {
        httpServletResponse.sendRedirect("https://www.giftipie.me/donation/cancel"); // TBD
    }
}