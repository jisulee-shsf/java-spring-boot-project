package com.giftforyoube.donation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giftforyoube.donation.dto.ReadyDonationRequestDto;
import com.giftforyoube.donation.dto.ReadyDonationResponseDto;
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
import java.util.Map;

@Slf4j
@Service
public class DonationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DonationService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Value("${kakaopay.cid}")
    private String kakaopayCid;
    @Value("${kakaopay.secret.key}")
    private String kakaopaySecretKey;
    @Value("${kakaopay.approval.redirect.url}")
    private String kakaopayApprovalRedirectUrl;
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

        Map<String, String> body = new LinkedHashMap<>();
        body.put("cid", kakaopayCid);
        body.put("partner_order_id", "partner_order_id");
        body.put("partner_user_id", "partner_user_id");
        body.put("item_name", "🥧 Giftipie 🥧");
        body.put("quantity", "1");
        body.put("total_amount", readyDonationRequestDto.getTotalAmount());
        body.put("vat_amount", "0");
        body.put("tax_free_amount", "0");
        body.put("approval_url", kakaopayApprovalRedirectUrl);
        body.put("cancel_url", kakaopayCancelRedirectUrl);
        body.put("fail_url", kakaopayFailRedirectUrl);
        readyDonationRequestDetails(uri, headers, body);

        RequestEntity<Map<String, String>> requestEntity = RequestEntity
                .post(uri)
                .headers(headers)
                .body(body);

        ResponseEntity<ReadyDonationResponseDto> responseEntity = restTemplate.exchange(
                requestEntity,
                ReadyDonationResponseDto.class);
        readyDonationResponseDetails(responseEntity);

        ReadyDonationResponseDto readyDonationResponseDto = responseEntity.getBody();
        return readyDonationResponseDto;
    }

    private void readyDonationRequestDetails(URI uri, HttpHeaders headers, Map<String, String> body) throws JsonProcessingException {
        log.info("[readyDonationRequestDetails] URL: " + uri);
        log.info("[readyDonationRequestDetails] Headers: " + objectMapper.writeValueAsString(headers));
        log.info("[readyDonationRequestDetails] Body: " + objectMapper.writeValueAsString(body));
    }

    private void readyDonationResponseDetails(ResponseEntity<?> responseEntity) throws JsonProcessingException {
        log.info("[readyDonationResponseDetails] Status Code: " + responseEntity.getStatusCode());
        log.info("[readyDonationResponseDetails] Headers: " + objectMapper.writeValueAsString(responseEntity.getHeaders()));
        log.info("[readyDonationResponseDetails] Body: " + objectMapper.writeValueAsString(responseEntity.getBody()));
    }
}