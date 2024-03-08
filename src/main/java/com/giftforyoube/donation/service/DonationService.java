package com.giftforyoube.donation.service;

import com.giftforyoube.donation.dto.ApproveDonationResponseDto;
import com.giftforyoube.donation.dto.ReadyDonationDto;
import com.giftforyoube.donation.entity.Donation;
import com.giftforyoube.donation.repository.DonationRepository;
import com.giftforyoube.funding.entity.Funding;
import com.giftforyoube.funding.entity.FundingStatus;
import com.giftforyoube.funding.entity.FundingSummary;
import com.giftforyoube.funding.repository.FundingRepository;
import com.giftforyoube.funding.repository.FundingSummaryRepository;
import com.giftforyoube.funding.service.CacheService;
import com.giftforyoube.global.exception.BaseException;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.global.security.UserDetailsImpl;
import com.giftforyoube.notification.entity.NotificationType;
import com.giftforyoube.notification.service.NotificationService;
import com.giftforyoube.user.entity.User;
import com.giftforyoube.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class DonationService {

    private final RestTemplate restTemplate;
    private final DonationRepository donationRepository;
    private final UserRepository userRepository;
    private final FundingRepository fundingRepository;
    private final FundingSummaryRepository fundingSummaryRepository;
    private final NotificationService notificationService;
    private final CacheService cacheService;

    @Value("${kakaopay.cid}")
    private String cid;
    @Value("${kakaopay.secret.key}")
    private String secretKey;
    @Value("${kakaopay.approve.redirect.url}")
    private String approveRedirectUrl;
    @Value("${kakaopay.cancel.redirect.url}")
    private String cancelRedirectUrl;
    @Value("${kakaopay.fail.redirect.url}")
    private String failRedirectUrl;

    /**
     * 1. 후원 랭킹 조회
     *
     * @param fundingId 펀딩 ID
     * @return 후원 랭킹
     */
    public int getDonationRanking(Long fundingId) {
        return calculateDonationRanking(fundingId);
    }

    /**
     * 2-1. 후원 결제 준비
     *
     * @param requestDto 결제 준비 요청 DTO
     * @return 결제 준비 응답 DTO
     */
    public ReadyDonationDto.ReadyDonationResponseDto readyDonation(ReadyDonationDto.ReadyDonationRequestDto requestDto) {
        log.info("[readyDonation] 후원 결제 준비 시도");

        URI uri = buildUri("/online/v1/payment/ready");
        HttpHeaders httpHeaders = buildHeaders();
        Map<String, Object> body = buildReadyRequestBody(requestDto.getDonation());

        RequestEntity<Map<String, Object>> requestEntity = RequestEntity
                .post(uri)
                .headers(httpHeaders)
                .body(body);

        ResponseEntity<ReadyDonationDto.ReadyDonationResponseDto> responseEntity =
                restTemplate.exchange(requestEntity, ReadyDonationDto.ReadyDonationResponseDto.class);
        ReadyDonationDto.ReadyDonationResponseDto responseBody = responseEntity.getBody();

        log.info("[readyDonation] 후원 결제 준비 완료");

        return ReadyDonationDto.ReadyDonationResponseDto.builder()
                .tid(responseBody.getTid())
                .next_redirect_pc_url(responseBody.getNext_redirect_pc_url())
                .next_redirect_mobile_url(responseBody.getNext_redirect_mobile_url())
                .build();
    }

    /**
     * 2-2. 후원 결제 승인
     *
     * @param tid 결제 고유 번호
     * @param pgToken 결제 승인 요청을 인증하는 토큰
     * @param sponsorNickname 후원자 닉네임
     * @param sponsorComment 후원자 코멘트
     * @param fundingId 펀딩 ID
     * @param userDetails 현재 유저의 UserDetailsImpl 객체
     */
    public void approveDonation(String tid, String pgToken,
                                String sponsorNickname, String sponsorComment,
                                Long fundingId, UserDetailsImpl userDetails) {
        log.info("[approveDonation] 후원 결제 승인 시도");

        URI uri = buildUri("/online/v1/payment/approve");
        HttpHeaders headers = buildHeaders();
        Map<String, Object> body = buildApproveRequestBody(tid, pgToken);

        RequestEntity<Map<String, Object>> requestEntity = RequestEntity
                .post(uri)
                .headers(headers)
                .body(body);

        ResponseEntity<ApproveDonationResponseDto> responseEntity =
                restTemplate.exchange(requestEntity, ApproveDonationResponseDto.class);
        ApproveDonationResponseDto approveDonationResponseDto = responseEntity.getBody();

        saveDonationInfo(sponsorNickname, sponsorComment,
                approveDonationResponseDto.getAmount().getTotal(), fundingId, userDetails);
        log.info("[approveDonation] 후원 결제 승인 완료");
    }

    /**
     * 3. 후원 정보 저장 및 관련 처리 진행
     *
     * @param sponsorNickname 후원자 닉네임
     * @param sponsorComment 후원자 코멘트
     * @param donationAmount 후원 금액
     * @param fundingId 펀딩 ID
     * @param userDetails 현재 유저의 UserDetailsImpl 객체
     */
    private void saveDonationInfo(String sponsorNickname, String sponsorComment,
                                  int donationAmount, Long fundingId, UserDetailsImpl userDetails) {
        // fundingId 기반 펀딩 확인
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.FUNDING_NOT_FOUND));

        // 후원 유저 확인
        User user = null;
        if (userDetails != null) {
            Long userId = userDetails.getUser().getId();
            user = userRepository.findById(userId).orElse(null);
        }

        // 후원 생성 및 DB 내 저장
        Donation donation = Donation.builder()
                .sponsorNickname(sponsorNickname)
                .sponsorComment(sponsorComment)
                .donationAmount(donationAmount)
                .donationRanking(calculateDonationRanking(fundingId))
                .funding(funding)
                .user(user)
                .build();
        donationRepository.save(donation);

        // 후원 누적 금액 업데이트
        int currentAmount = funding.getCurrentAmount() + donationAmount;
        funding.setCurrentAmount(currentAmount);
        fundingRepository.save(funding);

        // 펀딩 상태에 따라 통계 업데이트 및 알림 발송
        if (funding.getStatus().equals(FundingStatus.FINISHED)) {
            updateStatisticsForSuccessfulFunding();
            sendSuccessfulNotification(fundingId);
        }
        updateStatisticsForNewDonation(donationAmount);
        cacheService.clearFundingCaches();
    }

    /**
     * 4. 후원 랭킹 계산
     *
     * @param fundingId 펀딩 ID
     * @return 후원 랭킹
     */
    private int calculateDonationRanking(Long fundingId) {
        List<Donation> donations = donationRepository.findByFundingIdOrderByDonationRankingDesc(fundingId);
        if (donations.isEmpty()) {
            return 1;
        } else {
            int lastDonationRanking = donations.get(0).getDonationRanking();
            return lastDonationRanking + 1;
        }
    }

    /**
     * 5-1. URI 생성
     *
     * @param path URI 경로
     * @return 생성된 URI
     */
    private URI buildUri(String path) {
        return UriComponentsBuilder
                .fromUriString("https://open-api.kakaopay.com")
                .path(path)
                .encode()
                .build()
                .toUri();
    }

    /**
     * 5-2. HTTP 요청 헤더 생성
     *
     * @return 생성된 HTTP 헤더
     */
    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "SECRET_KEY " + secretKey);
        headers.add("Content-Type", "application/json");
        return headers;
    }

    /**
     * 5-3. 후원 결제 준비 요청 바디 생성
     *
     * @param donationAmount 후원 금액
     * @return 생성된 요청 바디
     */
    private Map<String, Object> buildReadyRequestBody(int donationAmount) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("cid", cid);
        body.put("partner_order_id", "partner_order_id");
        body.put("partner_user_id", "partner_user_id");
        body.put("item_name", "🥧 Giftipie 🥧");
        body.put("quantity", "1");
        body.put("total_amount", donationAmount);
        body.put("vat_amount", "0");
        body.put("tax_free_amount", "0");
        body.put("approval_url", approveRedirectUrl);
        body.put("cancel_url", cancelRedirectUrl);
        body.put("fail_url", failRedirectUrl);
        return body;
    }

    /**
     * 5-4. 후원 결제 승인 요청 바디 생성
     *
     * @param tid 결제 고유 번호
     * @param pgToken 결제 승인 요청을 인증하는 토큰
     * @return 생성된 요청 바디
     */
    private Map<String, Object> buildApproveRequestBody(String tid, String pgToken) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("cid", cid);
        body.put("tid", tid);
        body.put("partner_order_id", "partner_order_id");
        body.put("partner_user_id", "partner_user_id");
        body.put("pg_token", pgToken);
        return body;
    }

    /**
     * 6. 펀딩 ID에 해당하는 후원 목록 조회
     *
     * @param fundingId 후원을 조회할 펀딩의 ID
     * @return 펀딩 ID에 해당하는 후원 목록
     */
    public List<Donation> getDonationsByFundingId(Long fundingId) {
        return donationRepository.findByFundingId(fundingId);
    }

    /**
     * 7-1. 후원 결제 승인 후 알림 메시지 발송
     *
     * @param sponsorNickname 후원자의 닉네임
     * @param fundingId 후원이 발생한 펀딩의 ID
     */
    public void sendDonationNotification(String sponsorNickname, Long fundingId) {
        // 후원 결제 승인 후 알림 발송
        log.info("후원 결제 승인 후 알림 발송 시작");

        User user = userRepository.findUserByFundingId(fundingId);
        String content = String.format("회원님 펀딩에 %s 님이 후원하셨습니다!", sponsorNickname);
        String url = "https://www.giftipie.me/fundingdetail/" + fundingId;
        NotificationType notificationType = NotificationType.DONATION;
        notificationService.send(user, notificationType, content, url);
    }

    /**
     * 7-2. 펀딩 성공 시 알림 메시지 발송
     *
     * @param fundingId 펀딩의 ID
     */
    public void sendSuccessfulNotification(Long fundingId) {
        // 펀딩 성공 시 알림 발송
        log.info("펀딩 성공 시 알림 발송");

        User user = userRepository.findUserByFundingId(fundingId);
        String content = String.format("회원님의 선물펀딩이 목표금액에 달성되어 마감되었습니다!");
        String url = "https://www.giftipie.me/fundingdetail/" + fundingId;
        NotificationType notificationType = NotificationType.FUNDING_SUCCESS;
        notificationService.send(user, notificationType, content, url);
    }

    /**
     * 8-1. 후원 발생 시 통계 업데이트
     *
     * @param donationAmount 후원 금액
     */
    // 후원 발생시 summary 에 데이터 추가하는 메서드
    private void updateStatisticsForNewDonation(int donationAmount) {
        FundingSummary summary = fundingSummaryRepository.findFirstByOrderByIdAsc().orElse(new FundingSummary());
        summary.setTotalDonationsCount(summary.getTotalDonationsCount() + 1);
        summary.setTotalFundingAmount(summary.getTotalFundingAmount() + donationAmount);
        fundingSummaryRepository.save(summary);
    }

    /**
     * 8-2. 펀딩 성공 시 통계 업데이트
     */
    private void updateStatisticsForSuccessfulFunding() {
        FundingSummary summary = fundingSummaryRepository.findFirstByOrderByIdAsc().orElse(new FundingSummary());
        summary.setSuccessfulFundingsCount(summary.getSuccessfulFundingsCount() + 1);
        fundingSummaryRepository.save(summary);
    }
}