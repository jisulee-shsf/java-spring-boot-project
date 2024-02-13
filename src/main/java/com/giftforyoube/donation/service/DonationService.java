package com.giftforyoube.donation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.giftforyoube.donation.dto.ApproveDonationResponseDto;
import com.giftforyoube.donation.dto.DonationInfoDto;
import com.giftforyoube.donation.dto.ReadyDonationRequestDto;
import com.giftforyoube.donation.dto.ReadyDonationResponseDto;
import com.giftforyoube.donation.entity.Donation;
import com.giftforyoube.donation.repository.DonationRepository;
import com.giftforyoube.funding.entity.Funding;
import com.giftforyoube.funding.repository.FundingRepository;
import com.giftforyoube.global.security.UserDetailsImpl;
import com.giftforyoube.user.entity.User;
import com.giftforyoube.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DonationService {

    private final RestTemplate restTemplate;
    private final DonationRepository donationRepository;
    private final UserRepository userRepository;
    private final FundingRepository fundingRepository;

    public DonationService(RestTemplate restTemplate, DonationRepository donationRepository, UserRepository userRepository, FundingRepository fundingRepository) {
        this.restTemplate = restTemplate;
        this.donationRepository = donationRepository;
        this.userRepository = userRepository;
        this.fundingRepository = fundingRepository;
    }

    @Value("${kakaopay.cid}")
    private String kakaopayCid;
    @Value("${kakaopay.secret.key}")
    private String kakaopaySecretKey;
    @Value("${kakaopay.approve.redirect.url}")
    private String kakaopayApproveRedirectUrl;
    @Value("${kakaopay.cancel.redirect.url}")
    private String kakaopayCancelRedirectUrl;
    @Value("${kakaopay.fail.redirect.url}")
    private String kakaopayFailRedirectUrl;

    public ReadyDonationResponseDto readyDonation(ReadyDonationRequestDto readyDonationRequestDto) throws JsonProcessingException {
        URI uri = UriComponentsBuilder
                .fromUriString("https://open-api.kakaopay.com")
                .path("/online/v1/payment/ready")
                .encode()
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "SECRET_KEY " + kakaopaySecretKey);
        headers.add("Content-Type", "application/json");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("cid", kakaopayCid);
        body.put("partner_order_id", "partner_order_id");
        body.put("partner_user_id", "partner_user_id");
        body.put("item_name", "🥧 Giftipie 🥧");
        body.put("quantity", "1");
        body.put("total_amount", readyDonationRequestDto.getDonation());
        body.put("vat_amount", "0");
        body.put("tax_free_amount", "0");
        body.put("approval_url", kakaopayApproveRedirectUrl);
        body.put("cancel_url", kakaopayCancelRedirectUrl);
        body.put("fail_url", kakaopayFailRedirectUrl);

        RequestEntity<Map<String, Object>> requestEntity = RequestEntity
                .post(uri)
                .headers(headers)
                .body(body);

        ResponseEntity<ReadyDonationResponseDto> responseEntity = restTemplate.exchange(
                requestEntity,
                ReadyDonationResponseDto.class);

        ReadyDonationResponseDto readyDonationResponseDto = responseEntity.getBody();
        return readyDonationResponseDto;
    }

    public DonationInfoDto approveDonation(String tid, String pgToken, String sponsorNickname, String comment, Long fundingId, UserDetailsImpl userDetails) throws JsonProcessingException {
        URI uri = UriComponentsBuilder
                .fromUriString("https://open-api.kakaopay.com")
                .path("/online/v1/payment/approve")
                .encode()
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "SECRET_KEY " + kakaopaySecretKey);
        headers.add("Content-Type", "application/json");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("cid", kakaopayCid);
        body.put("tid", tid);
        body.put("partner_order_id", "partner_order_id");
        body.put("partner_user_id", "partner_user_id");
        body.put("pg_token", pgToken);

        RequestEntity<Map<String, Object>> requestEntity = RequestEntity
                .post(uri)
                .headers(headers)
                .body(body);

        ResponseEntity<ApproveDonationResponseDto> responseEntity = restTemplate.exchange(
                requestEntity,
                ApproveDonationResponseDto.class);

        ApproveDonationResponseDto approveDonationResponseDto = responseEntity.getBody();
        Donation donation = saveDonationInfo(sponsorNickname, comment, approveDonationResponseDto.getAmount().getTotal(), fundingId, userDetails);
        DonationInfoDto donationInfoDto = new DonationInfoDto(donation.getSponsorNickname(), donation.getComment(), donation.getDonationAmount(), donation.getDonationRanking());
        return donationInfoDto;
    }

    private Donation saveDonationInfo(String sponsorNickname, String comment, int donationAmount, Long fundingId, UserDetailsImpl userDetails) throws JsonProcessingException {
        int donationRanking = calculateDonationRanking(fundingId);

        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new IllegalArgumentException("펀딩 정보를 찾을 수 없습니다."));

        User user = null;
        if (userDetails != null) {
            Long userId = userDetails.getUser().getId();
            user = userRepository.findById(userId).orElse(null);
        }

        Donation donation = new Donation(sponsorNickname, comment, donationAmount, donationRanking, funding, user);
        donationRepository.save(donation);
        return donation;
    }

    private int calculateDonationRanking(Long fundingId) {
        List<Donation> donations = donationRepository.findByFundingIdOrderByDonationRankingDesc(fundingId);
        if (donations.isEmpty()) {
            return 1;
        } else {
            int lastDonationRanking = donations.get(0).getDonationRanking();
            return lastDonationRanking + 1;
        }
    }
}