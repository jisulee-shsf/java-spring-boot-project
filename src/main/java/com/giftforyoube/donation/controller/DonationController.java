package com.giftforyoube.donation.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.giftforyoube.donation.dto.GetDonationInfoResponseDto;
import com.giftforyoube.donation.dto.GetDonationRankingResponseDto;
import com.giftforyoube.donation.dto.ReadyDonationRequestDto;
import com.giftforyoube.donation.dto.ReadyDonationResponseDto;
import com.giftforyoube.donation.entity.Donation;
import com.giftforyoube.donation.service.DonationService;
import com.giftforyoube.global.exception.BaseResponse;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.global.security.UserDetailsImpl;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class DonationController {

    private final DonationService donationService;
    private final HttpSession session;

    public DonationController(DonationService donationService, HttpSession session) {
        this.donationService = donationService;
        this.session = session;
    }

    @Value("${giftipie.redirect.url}")
    private String giftipieRedirectUrl;

    // 1. 후원 랭킹 조회
    @GetMapping("/funding/{Id}/donation")
    public ResponseEntity<BaseResponse<GetDonationRankingResponseDto>> getDonationRanking(@PathVariable("Id") Long id) {
        GetDonationRankingResponseDto getDonationRankingResponseDto = new GetDonationRankingResponseDto(donationService.getDonationRanking(id));
        BaseResponse<GetDonationRankingResponseDto> baseResponse = new BaseResponse<>(BaseResponseStatus.SUCCESS, getDonationRankingResponseDto);
        return ResponseEntity.status(HttpStatus.OK).body(baseResponse); // 200
    }

    // 2. 후원 결제 준비
    @PostMapping("/funding/{id}/donation/ready")
    public ResponseEntity<BaseResponse<ReadyDonationResponseDto>> readyDonation(@PathVariable Long id,
                                                                                @RequestBody ReadyDonationRequestDto readyDonationRequestDto) {
        ReadyDonationResponseDto readyDonationResponseDto = donationService.readyDonation(readyDonationRequestDto);
        session.setAttribute("fundingId", id);
        session.setAttribute("sponsorNickname", readyDonationRequestDto.getSponsorNickname());
        session.setAttribute("sponsorComment", readyDonationRequestDto.getSponsorComment());
        session.setAttribute("tid", readyDonationResponseDto.getTid());

        BaseResponse<ReadyDonationResponseDto> baseResponse = new BaseResponse<>(BaseResponseStatus.DONATION_READY_SUCCESS, readyDonationResponseDto);
        return ResponseEntity.status(HttpStatus.OK).body(baseResponse);
    }

    // 3-1. 후원 결제 승인
    @GetMapping("/donation/approve")
    public ResponseEntity<BaseResponse<GetDonationInfoResponseDto>> approveDonation(@RequestParam("pg_token") String pgToken,
                                                                                    @AuthenticationPrincipal UserDetailsImpl userDetails) throws JsonProcessingException {
        String tid = (String) session.getAttribute("tid");
        String sponsorNickname = (String) session.getAttribute("sponsorNickname");
        String sponsorComment = (String) session.getAttribute("sponsorComment");
        Long fundingId = (Long) session.getAttribute("fundingId");
        GetDonationInfoResponseDto getDonationInfoResponseDto = donationService.approveDonation(tid, pgToken, sponsorNickname, sponsorComment, fundingId, userDetails);

//        String redirectUrl = giftipieRedirectUrl + "fundingdetail/" + fundingId;
        BaseResponse<GetDonationInfoResponseDto> baseResponse = new BaseResponse<>(BaseResponseStatus.DONATION_APPROVE_SUCCESS, getDonationInfoResponseDto);
        return ResponseEntity.status(HttpStatus.OK)
//                .location(new URI(redirectUrl))
                .body(baseResponse);
    }

    // 3-2. 후원 결제 실패
    @GetMapping("/donation/fail")
    public ResponseEntity<BaseResponse> failDonation() {
        BaseResponse<Void> baseResponse = new BaseResponse<>(BaseResponseStatus.DONATION_FAIL);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(baseResponse);
    }

    // 3-3. 후원 결제 취소
    @GetMapping("/donation/cancel")
    public ResponseEntity<BaseResponse> cancelDonation() {
        BaseResponse<Void> baseResponse = new BaseResponse<>(BaseResponseStatus.DONATION_CANCEL);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(baseResponse);
    }

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