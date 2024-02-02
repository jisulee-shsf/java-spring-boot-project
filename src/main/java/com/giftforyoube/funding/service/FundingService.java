package com.giftforyoube.funding.service;

import com.giftforyoube.funding.dto.FundingCreateRequestDto;
import com.giftforyoube.funding.dto.FundingResponseDto;
import com.giftforyoube.funding.entity.Funding;
import com.giftforyoube.funding.entity.FundingItem;
import com.giftforyoube.funding.entity.FundingStatus;
import com.giftforyoube.funding.repository.FundingRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Getter
@RequiredArgsConstructor
public class FundingService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final FundingRepository fundingRepository;

    private static final int TIMEOUT = 10000; // 10초

    public void saveToCache(FundingItem fundingItem) {
        redisTemplate.opsForValue().set("cachedFundingItem", fundingItem);
    }

    public FundingItem getCachedFundingProduct() {
        return (FundingItem) redisTemplate.opsForValue().get("cachedFundingItem");
    }

    public FundingItem previewItem(String itemLink) throws IOException {
        Document document = Jsoup.connect(itemLink).timeout(TIMEOUT).get();
        String itemImage = getMetaTagContent(document, "og:image");
        if (itemImage == null) {
            throw new IOException("Cannot fetch item image.");
        }
        return FundingItem.builder()
                .itemLink(itemLink)
                .itemImage(itemImage)
                .build();
    }

    public void clearCache() {
        redisTemplate.delete("cachedFundingItem");
    }

    private static String getMetaTagContent(Document document, String property) {
        Element metaTag = document.select("meta[property=" + property + "]").first();
        return (metaTag != null) ? metaTag.attr("content") : null;
    }

    @Transactional
    public FundingResponseDto saveToDatabase(FundingCreateRequestDto requestDto) {
        FundingItem fundingItem = getCachedFundingProduct();
        if (fundingItem == null) {
            throw new IllegalStateException("No cached funding item found.");
        }
        LocalDate currentDate = LocalDate.now();
        FundingStatus status = requestDto.getEndDate().isBefore(currentDate) ? FundingStatus.FINISHED : FundingStatus.ACTIVE;
        Funding funding = requestDto.toEntity(fundingItem,status);
        fundingRepository.save(funding);
        clearCache();
        return FundingResponseDto.fromEntity(funding);
    }

    public FundingResponseDto findFunding(Long fundingId) {
        // 캐시에서 펀딩 정보 조회 시도
        FundingResponseDto fundingResponse = getCachedFundingInfo(fundingId);
        if (fundingResponse != null) {
            return fundingResponse;
        }

        // 캐시에 정보가 없는 경우 DB에서 조회 및 계산
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new NullPointerException("해당 펀딩을 찾을 수 없습니다."));
        fundingResponse = FundingResponseDto.fromEntity(funding);

        // 계산된 정보를 캐시에 저장
        cacheFundingInfo(fundingId, fundingResponse);
        return fundingResponse;
    }

    // 해당 유저의 펀딩 정보 캐시에서 가져오기
    private FundingResponseDto getCachedFundingInfo(Long fundingId) {
        return (FundingResponseDto) redisTemplate.opsForValue().get("funding:" + fundingId + ":info");
    }

    // 캐시에 해당 유저의 펀딩 정보 저장
    private void cacheFundingInfo(Long fundingId, FundingResponseDto fundingInfo) {
        redisTemplate.opsForValue().set("funding:" + fundingId + ":info", fundingInfo, 1, TimeUnit.DAYS);
    }

    @Transactional(readOnly = true)
    public List<Funding> getActiveFundings() {
        // 캐시에서 진행 중인 펀딩 목록 조회 시도
        List<Funding> activeFundings = (List<Funding>) redisTemplate.opsForValue().get("activeFundings");
        if (activeFundings == null) {
            // 캐시에 없으면 데이터베이스에서 조회
            LocalDate currentDate = LocalDate.now();
            activeFundings = fundingRepository.findByEndDateGreaterThanEqualAndStatus(currentDate, FundingStatus.ACTIVE);
            // 조회 결과를 캐시에 저장
            redisTemplate.opsForValue().set("activeFundings", activeFundings, 1, TimeUnit.HOURS);
        }
        return activeFundings;
    }

    @Transactional(readOnly = true)
    public List<Funding> getFinishedFunding() {
        // 캐시에서 종료된 펀딩 목록 조회 시도
        List<Funding> finishedFundings = (List<Funding>) redisTemplate.opsForValue().get("finishedFundings");
        if (finishedFundings == null) {
            // 캐시에 없으면 데이터베이스에서 조회
            LocalDate currentDate = LocalDate.now();
            finishedFundings = fundingRepository.findByEndDateLessThanAndStatus(currentDate, FundingStatus.FINISHED);
            // 조회 결과를 캐시에 저장
            redisTemplate.opsForValue().set("finishedFundings", finishedFundings, 1, TimeUnit.HOURS);
        }
        return finishedFundings;
    }

    @Transactional
    public void finishFunding(Long fundingId) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new IllegalArgumentException("해당 펀딩을 찾을 수 없습니다."));
        funding.setStatus(FundingStatus.FINISHED);
        fundingRepository.save(funding);

        // 관련 캐시 무효화
        redisTemplate.delete("activeFundings");
        redisTemplate.delete("finishedFundings");
    }
}
