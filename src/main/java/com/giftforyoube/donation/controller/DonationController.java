package com.giftforyoube.donation.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.giftforyoube.donation.dto.*;
import com.giftforyoube.donation.entity.Donation;
import com.giftforyoube.donation.service.DonationService;
import com.giftforyoube.global.exception.BaseResponse;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.global.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "후원", description = "후원 관련 API")
public class DonationController {

    private final DonationService donationService;
    private final HttpSession session;
    private Long fundingId;

    // 1. 후원 랭킹 조회
    @GetMapping("/funding/{Id}/donation")
    public ResponseEntity<BaseResponse<GetDonationRankingResponseDto>> getDonationRanking(@PathVariable("Id") Long id) {
        GetDonationRankingResponseDto getDonationRankingResponseDto = new GetDonationRankingResponseDto(donationService.getDonationRanking(id));
        BaseResponse<GetDonationRankingResponseDto> baseResponse = new BaseResponse<>(BaseResponseStatus.SUCCESS, getDonationRankingResponseDto);
        return ResponseEntity.status(HttpStatus.OK).body(baseResponse);
    }

    // 2. 후원 결제 준비
    @PostMapping("/funding/{id}/donation/ready")
    public ResponseEntity<BaseResponse<ReadyDonationDto.ResponseDto>> readyDonation(@PathVariable Long id,
                                                                                    @RequestBody ReadyDonationDto.RequestDto requestDto) {
        try {
            ReadyDonationDto.ResponseDto responseDto = donationService.readyDonation(requestDto);

            this.fundingId = id;
            session.setAttribute("sponsorNickname", requestDto.getSponsorNickname());
            session.setAttribute("sponsorComment", requestDto.getSponsorComment());
            session.setAttribute("tid", responseDto.getTid());

            return ResponseEntity.status(HttpStatus.OK)
                    .body(new BaseResponse<>(BaseResponseStatus.DONATION_READY_SUCCESS, responseDto));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new BaseResponse<>(BaseResponseStatus.DONATION_READY_FAILED));
        }
    }

    // 3-1. 후원 결제 승인
    @GetMapping("/donation/approve")
    public ResponseEntity<BaseResponse<Void>> approveDonation(@RequestParam("pg_token") String pgToken,
                                                @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            String tid = (String) session.getAttribute("tid");
            String sponsorNickname = (String) session.getAttribute("sponsorNickname");
            String sponsorComment = (String) session.getAttribute("sponsorComment");

            donationService.approveDonation(tid, pgToken, sponsorNickname, sponsorComment, fundingId, userDetails);
            // 후원 결제 승인 시 알람 발송
            try {
                donationService.sendDonationNotification(sponsorNickname, fundingId);
            } catch (Exception e) {
                log.error("SSE 알림 error: ", e);
            }
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new BaseResponse<>(BaseResponseStatus.DONATION_APPROVE_SUCCESS));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new BaseResponse<>(BaseResponseStatus.DONATION_APPROVE_FAILED));
        }
    }

//    // 3-2. 후원 결제 실패
//    @GetMapping("/donation/fail")
//    public ResponseEntity<Void> failDonation() throws URISyntaxException {
//        HttpHeaders headers = new HttpHeaders();
//        headers.setLocation(new URI(giftipieRedirectUrl + "fundingdetail/" + fundingId));
//        return new ResponseEntity<>(headers, HttpStatus.FOUND);
//    }
//
//    // 3-3. 후원 결제 취소
//    @GetMapping("/donation/cancel")
//    public ResponseEntity<Void> cancelDonation() throws URISyntaxException {
//        HttpHeaders headers = new HttpHeaders();
//        headers.setLocation(new URI(giftipieRedirectUrl + "fundingdetail/" + fundingId));
//        return new ResponseEntity<>(headers, HttpStatus.FOUND);
//    }

    // 4-1. 후원 정보 리스트 조회
    @GetMapping("/funding/{id}/donations")
    public ResponseEntity<BaseResponse<List<GetDonationInfoResponseDto>>> getDonationsByFundingId(@PathVariable("id") Long fundingId) {
        List<Donation> donations = donationService.getDonationsByFundingId(fundingId);
        List<GetDonationInfoResponseDto> donationResponseDtos = mapDonationEntitiesToResponseDtos(donations);
        BaseResponse<List<GetDonationInfoResponseDto>> baseResponse = new BaseResponse<>(BaseResponseStatus.SUCCESS, donationResponseDtos);
        return ResponseEntity.status(HttpStatus.OK).body(baseResponse);
    }

    private List<GetDonationInfoResponseDto> mapDonationEntitiesToResponseDtos(List<Donation> donations) {
        return donations.stream()
                .map(donation -> new GetDonationInfoResponseDto(
                        donation.getSponsorNickname(),
                        donation.getSponsorComment(),
                        donation.getDonationRanking()))
                .collect(Collectors.toList());
    }
}