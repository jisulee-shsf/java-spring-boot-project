package com.giftforyoube.funding.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giftforyoube.funding.dto.*;
import com.giftforyoube.funding.entity.FundingItem;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.hibernate.query.sqm.tree.SqmNode.log;

@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;


    private static final String FUNDING_ITEM_CACHE_PREFIX = "cachedFundingItem:";
    private static final String FUNDING_SUMMARY_CACHE_KEY = "fundingSummary";

    public String buildCacheKey(String userId) {
        return FUNDING_ITEM_CACHE_PREFIX + userId;
    }

    // FundingItem 객체를 JSON으로 변환하여 캐시에 저장
    public void saveToCache(FundingItem fundingItem, String userId) throws JsonProcessingException {
        log.info("[saveToCache] 캐쉬에 저장하기");

        String cacheKey = buildCacheKey(userId);
        String fundingItemJson = objectMapper.writeValueAsString(fundingItem);
        redisTemplate.opsForValue().set(cacheKey, fundingItemJson, Duration.ofDays(1));
    }

    // 캐시에서 FundingItem 객체를 가져오기
    public FundingItem getCachedFundingProduct(String cacheKey) throws JsonProcessingException {
        log.info("[getCachedFundingProduct] 캐시에서 FundingItem 객체를 가져오기");

        String fundingItemJson = redisTemplate.opsForValue().get(cacheKey); // 역직렬화
        return fundingItemJson == null ? null : objectMapper.readValue(fundingItemJson, FundingItem.class);
    }


    public void clearCache(String userCacheKey) {
        log.info("[clearCache] 캐쉬 삭제하기");

        String cacheKey = buildCacheKey(userCacheKey);
        redisTemplate.delete(cacheKey);
    }

    //    // 캐시에 Page 데이터 저장
    public void saveFundingPageToCache(String cacheKey, Page<FundingResponseDto> page) {
        try {
            // Page 구현체를 JSON으로 변환하는 과정에서는 구현체의 구체적인 클래스 정보가 필요할 수 있으므로,
            // Page 내용만 캐시하고, 페이징 정보는 별도로 관리하는 것을 고려해야 할 수 있습니다.
            String jsonContent = objectMapper.writeValueAsString(page.getContent());
            redisTemplate.opsForValue().set(cacheKey, jsonContent, Duration.ofHours(1));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing funding page data", e);
        }
    }

    // 캐시에서 Page 데이터 조회
    public Page<FundingResponseDto> getFundingPageFromCache(String cacheKey, Pageable pageable) {
        String jsonContent = redisTemplate.opsForValue().get(cacheKey);
        if (jsonContent == null) {
            return null;
        }
        try {
            List<FundingResponseDto> content = objectMapper.readValue(jsonContent, new TypeReference<List<FundingResponseDto>>(){});
            // 여기서는 캐시된 내용과 Pageable 정보를 기반으로 새 Page 객체를 생성해야 합니다.
            // 실제 페이지 크기와 전체 페이지 수 등은 DB 조회 없이 알 수 없으므로, 이 부분은 적절히 조정이 필요합니다.
            return new PageImpl<>(content, pageable, content.size());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializing funding page data", e);
        }
    }
    // 캐시 관련 메서드 수정
    public void saveFundingsPageToCache(String cacheKey, Page<FundingResponseDto> page) {
        try {
            FundingPageCached<FundingResponseDto> cachedPage = new FundingPageCached<>();
            cachedPage.setContent(page.getContent());
            cachedPage.setMetadata(new FundingPageMetadata(page.getTotalPages(), page.getTotalElements()));

            String jsonContent = objectMapper.writeValueAsString(cachedPage);
            redisTemplate.opsForValue().set(cacheKey, jsonContent, Duration.ofHours(1));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing funding page data", e);
        }
    }

    public Page<FundingResponseDto> getFundingsPageFromCache(String cacheKey, Pageable pageable) {
        String jsonContent = redisTemplate.opsForValue().get(cacheKey);
        if (jsonContent == null) {
            return Page.empty(); // 캐시에서 데이터를 가져올 수 없으면 빈 페이지 반환
        }
        try {
            FundingPageCached<FundingResponseDto> cachedPage = objectMapper.readValue(jsonContent, new TypeReference<FundingPageCached<FundingResponseDto>>() {
            });
            return new PageImpl<>(cachedPage.getContent(), pageable, cachedPage.getMetadata().getTotalElements());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializing funding page data", e);
        }
    }


    // 캐시에 펀딩 목록 저장하는 로직
    public void saveFundingListToCache(String cacheKey, Slice<FundingResponseDto> fundings) {
        FundingResponseDtoCache cache = new FundingResponseDtoCache(
                new ArrayList<>(fundings.getContent()),
                fundings.getNumber(),
                fundings.getSize(),
                fundings.isLast()
        );

        try {
            String jsonContent = objectMapper.writeValueAsString(cache);
            redisTemplate.opsForValue().set(cacheKey, jsonContent, Duration.ofHours(1)); // 캐시 만료 시간은 필요에 따라 조정
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing funding data", e);
        }
    }

    // 캐시에서 목록 조회하는 로직
    public Slice<FundingResponseDto> getFundingListFromCache(String cacheKey, Pageable pageable) {
        String jsonContent = redisTemplate.opsForValue().get(cacheKey);
        if (jsonContent == null) {
            return new SliceImpl<>(Collections.emptyList(), pageable, false);
        }
        try {
            FundingResponseDtoCache cache = objectMapper.readValue(jsonContent, FundingResponseDtoCache.class);
            return new SliceImpl<>(cache.getContent(), PageRequest.of(cache.getPage(), cache.getSize()), cache.isLast());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializing funding data", e);
        }
    }

    // 펀딩 상세 정보 캐시에 저장
    public void saveFundingToCache(String cacheKey, FundingResponseDto fundingResponseDto) {
        try {
            String jsonContent = objectMapper.writeValueAsString(fundingResponseDto);
            redisTemplate.opsForValue().set(cacheKey, jsonContent, Duration.ofHours(1));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing funding detail", e);
        }
    }

    // 펀딩 상세 정보 캐시에서 조회
    public FundingResponseDto getFundingFromCache(String cacheKey) {
        String jsonContent = redisTemplate.opsForValue().get(cacheKey);
        if (jsonContent == null) {
            return null;
        }
        try {
            return objectMapper.readValue(jsonContent, FundingResponseDto.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializing funding detail", e);
        }
    }

    // Giftipie에서 함께한 선물 캐시에서 가져오기
    public FundingSummaryResponseDto getSummaryFromCache() {
        String jsonContent = redisTemplate.opsForValue().get(FUNDING_SUMMARY_CACHE_KEY);
        if (jsonContent == null) {
            return null;
        }
        try {
            return objectMapper.readValue(jsonContent, FundingSummaryResponseDto.class);
        } catch (JsonProcessingException e) {
            log.error("Error deserializing funding summary from cache", e);
            return null;
        }
    }

    // Giftipie에서 함께한 선물 캐시에 저장
    public void saveSummaryToCache(FundingSummaryResponseDto summary) {
        try {
            String jsonContent = objectMapper.writeValueAsString(summary);
            redisTemplate.opsForValue().set(FUNDING_SUMMARY_CACHE_KEY, jsonContent, Duration.ofHours(1)); // 캐시 유지 시간은 요구 사항에 따라 조정 가능
        } catch (JsonProcessingException e) {
            log.error("Error serializing funding summary to cache", e);
        }
    }

    // 펀딩 생성, 업데이트, 삭제 시 캐시 삭제
    public void clearFundingCaches() {
        // 메인 펀딩 관련 캐시 삭제
        clearCacheByPattern("activeMainFundings:*");

        // 기존의 펀딩 리스트 관련 캐시 삭제
        clearCacheByPattern("allFundings:*");
        clearCacheByPattern("activeFundings:*");
        clearCacheByPattern("finishedFundings:*");
        clearCacheByPattern("MyFundingInfo:*");

        // 상세 페이지 캐시 삭제 추가
        clearCacheByPattern("fundingDetail:*");

        // Giftipie에서 함께한 선물 캐시 삭제
        clearCacheByPattern(FUNDING_SUMMARY_CACHE_KEY);
    }

    public void clearCacheByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
