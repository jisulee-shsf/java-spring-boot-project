package com.giftforyoube.donation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.giftforyoube.donation.dto.*;
import com.giftforyoube.donation.entity.Donation;
import com.giftforyoube.donation.repository.DonationRepository;
import com.giftforyoube.funding.entity.Funding;
import com.giftforyoube.funding.repository.FundingRepository;
import com.giftforyoube.funding.service.FundingService;
import com.giftforyoube.global.exception.BaseException;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.global.security.UserDetailsImpl;
import com.giftforyoube.notification.entity.NotificationType;
import com.giftforyoube.notification.service.NotificationService;
import com.giftforyoube.user.entity.User;
import com.giftforyoube.user.repository.UserRepository;
import io.lettuce.core.dynamic.annotation.Param;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.Query;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DonationService {

    private final RestTemplate restTemplate;
    private final DonationRepository donationRepository;
    private final UserRepository userRepository;
    private final FundingRepository fundingRepository;
    private final FundingService fundingService;
    private final NotificationService notificationService;

    public DonationService(RestTemplate restTemplate, DonationRepository donationRepository,
                           UserRepository userRepository, FundingRepository fundingRepository,
                           FundingService fundingService, NotificationService notificationService) {
        this.restTemplate = restTemplate;
        this.donationRepository = donationRepository;
        this.userRepository = userRepository;
        this.fundingRepository = fundingRepository;
        this.fundingService = fundingService;
        this.notificationService = notificationService;
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

    public int getDonationRanking(Long fundingId) {
        int donationRanking = calculateDonationRanking(fundingId);
        return donationRanking;
    }

    public ReadyDonationResponseDto readyDonation(ReadyDonationRequestDto readyDonationRequestDto) {
        URI uri = buildKakaoPayUri("/online/v1/payment/ready");
        HttpHeaders headers = buildKakaoPayHeaders();
        Map<String, Object> body = buildKakaoPayReadyRequestBody(readyDonationRequestDto.getDonation());

        RequestEntity<Map<String, Object>> requestEntity = RequestEntity
                .post(uri)
                .headers(headers)
                .body(body);

        ResponseEntity<ReadyDonationResponseDto> responseEntity = restTemplate.exchange(requestEntity, ReadyDonationResponseDto.class);
        ReadyDonationResponseDto readyDonationResponseDto = responseEntity.getBody();
        log.info("[readyDonation] 후원 결제준비 완료");
        return readyDonationResponseDto;
    }

    public void approveDonation(String tid, String pgToken, String sponsorNickname, String sponsorComment, Long fundingId, UserDetailsImpl userDetails) throws JsonProcessingException {
        URI uri = buildKakaoPayUri("/online/v1/payment/approve");
        HttpHeaders headers = buildKakaoPayHeaders();
        Map<String, Object> body = buildKakaoPayApproveRequestBody(tid, pgToken);

        RequestEntity<Map<String, Object>> requestEntity = RequestEntity
                .post(uri)
                .headers(headers)
                .body(body);

        ResponseEntity<ApproveDonationResponseDto> responseEntity = restTemplate.exchange(requestEntity, ApproveDonationResponseDto.class);
        ApproveDonationResponseDto approveDonationResponseDto = responseEntity.getBody();
        saveDonationInfo(sponsorNickname, sponsorComment, approveDonationResponseDto.getAmount().getTotal(), fundingId, userDetails);
        log.info("[approveDonation] 후원 결제승인 완료: " + fundingId + " 번 펀딩에 " + sponsorNickname + "님이 "+ approveDonationResponseDto.getAmount().getTotal() + "원을 후원하셨습니다.");
    }

    private URI buildKakaoPayUri(String path) {
        return UriComponentsBuilder
                .fromUriString("https://open-api.kakaopay.com")
                .path(path)
                .encode()
                .build()
                .toUri();
    }

    private HttpHeaders buildKakaoPayHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "SECRET_KEY " + kakaopaySecretKey);
        headers.add("Content-Type", "application/json");
        return headers;
    }

    private Map<String, Object> buildKakaoPayReadyRequestBody(int donationAmount) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("cid", kakaopayCid);
        body.put("partner_order_id", "partner_order_id");
        body.put("partner_user_id", "partner_user_id");
        body.put("item_name", "🥧 Giftipie 🥧");
        body.put("quantity", "1");
        body.put("total_amount", donationAmount);
        body.put("vat_amount", "0");
        body.put("tax_free_amount", "0");
        body.put("approval_url", kakaopayApproveRedirectUrl);
        body.put("cancel_url", kakaopayCancelRedirectUrl);
        body.put("fail_url", kakaopayFailRedirectUrl);
        return body;
    }

    private Map<String, Object> buildKakaoPayApproveRequestBody(String tid, String pgToken) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("cid", kakaopayCid);
        body.put("tid", tid);
        body.put("partner_order_id", "partner_order_id");
        body.put("partner_user_id", "partner_user_id");
        body.put("pg_token", pgToken);
        return body;
    }

    private void saveDonationInfo(String sponsorNickname, String sponsorComment, int donationAmount, Long fundingId, UserDetailsImpl userDetails) {
        try {
            Funding funding = fundingRepository.findById(fundingId)
                    .orElseThrow(IllegalArgumentException::new);

            int donationRanking = calculateDonationRanking(fundingId);

            User user = null;
            if (userDetails != null) {
                Long userId = userDetails.getUser().getId();
                user = userRepository.findById(userId).orElse(null);
            }
            Donation donation = new Donation(sponsorNickname, sponsorComment, donationAmount, donationRanking, funding, user);
            donationRepository.save(donation);
            fundingService.clearFundingCaches();
        } catch (IllegalArgumentException e) {
            throw new BaseException(BaseResponseStatus.FUNDING_NOT_FOUND);
        }
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

    public List<Donation> getDonationsByFundingId(Long fundingId) {
        return donationRepository.findByFundingId(fundingId);
    }

    // 후원 결제 승인 시 알림메시지 발송
    @Async
    public void sendDonationNotification (String sponsorNickname, Long fundingId) {
        // 후원 결제 승인 후 알림 발송
        log.info("후원 결제 승인 후 알림 발송 시작");

        User user = userRepository.findUserByFundingId(fundingId);
//        String content = "님 펀딩에 " + sponsorNickname + "님이 후원하셨습니다!";
        String content = String.format("회원님 펀딩에 %s 님이 후원하셨습니다!", sponsorNickname);
        String url = "https://www.giftipie.me/fundingdetail/" + fundingId;
        NotificationType notificationType = NotificationType.DONATION;
        notificationService.send(user, notificationType, content, url);
    }
}