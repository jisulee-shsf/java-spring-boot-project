package com.giftforyoube.funding.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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

    private final RedisTemplate<String, String> redisTemplate;
    private final FundingRepository fundingRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    private CacheManager cacheManager;

    private static final int TIMEOUT = 10000; // 10초

    // FundingItem 객체를 JSON으로 변환하여 캐시에 저장
    public void saveToCache(FundingItem fundingItem) throws JsonProcessingException {
        String fundingItemJson = objectMapper.writeValueAsString(fundingItem);
        redisTemplate.opsForValue().set("cachedFundingItem", fundingItemJson,1, TimeUnit.DAYS);
    }

    // 캐시에서 FundingItem 객체를 가져오기
    public FundingItem getCachedFundingProduct() throws JsonMappingException, JsonProcessingException {
        String fundingItemJson = redisTemplate.opsForValue().get("cachedFundingItem");
        return fundingItemJson == null ? null : objectMapper.readValue(fundingItemJson, FundingItem.class);
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
    public FundingResponseDto saveToDatabase(FundingCreateRequestDto requestDto) throws JsonProcessingException {
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

    @Cacheable(value = "fundingInfoCache", key = "#fundingId")
    public FundingResponseDto findFunding(Long fundingId){
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
        Cache cache = cacheManager.getCache("fundingInfoCache");
        Cache.ValueWrapper wrapper = cache.get(fundingId);
        if (wrapper != null) {
            return (FundingResponseDto) wrapper.get();
        }
        return null;
    }

    // 캐시에 해당 유저의 펀딩 정보 저장
    private void cacheFundingInfo(Long fundingId, FundingResponseDto fundingInfo) {
        Cache cache = cacheManager.getCache("fundingInfoCache");
        if (cache != null) {
            cache.put(fundingId, fundingInfo);
        }
    }


    @Cacheable("activeFundings")
    @Transactional(readOnly = true)
    public List<Funding> getActiveFundings() {
        LocalDate currentDate = LocalDate.now();
        return fundingRepository.findByEndDateGreaterThanEqualAndStatus(currentDate, FundingStatus.ACTIVE);
    }

    @Cacheable("finishedFundings")
    @Transactional(readOnly = true)
    public List<Funding> getFinishedFunding() {
        LocalDate currentDate = LocalDate.now();
        return fundingRepository.findByEndDateLessThanAndStatus(currentDate, FundingStatus.FINISHED);
    }

    @CacheEvict(value = {"activeFundings", "finishedFundings"}, allEntries = true, beforeInvocation = true)
    @Transactional
    public void finishFunding(Long fundingId) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new IllegalArgumentException("해당 펀딩을 찾을 수 없습니다."));
        funding.setStatus(FundingStatus.FINISHED);
        // fundingRepository.save(funding); // 저장은 필요하지 않음
        cacheFundingInfo(fundingId, FundingResponseDto.fromEntity(funding));
    }
}
