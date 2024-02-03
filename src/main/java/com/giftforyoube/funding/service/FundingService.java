package com.giftforyoube.funding.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giftforyoube.funding.dto.FundingCreateRequestDto;
import com.giftforyoube.funding.dto.FundingResponseDto;
import com.giftforyoube.funding.entity.Funding;
import com.giftforyoube.funding.entity.FundingItem;
import com.giftforyoube.funding.entity.FundingStatus;
import com.giftforyoube.funding.repository.FundingRepository;
import com.giftforyoube.user.entity.User;
import com.giftforyoube.user.repository.UserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Getter
@RequiredArgsConstructor
public class FundingService {

    private final RedisTemplate<String, String> redisTemplate;
    private final FundingRepository fundingRepository;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    private static final int TIMEOUT = 10000; // 10초

    // FundingItem 객체를 JSON으로 변환하여 캐시에 저장
    public void saveToCache(FundingItem fundingItem,String userCacheKey) throws JsonProcessingException {
        String cacheKey = "cachedFundingItem:" + userCacheKey;
        String fundingItemJson = objectMapper.writeValueAsString(fundingItem);
        redisTemplate.opsForValue().set(cacheKey, fundingItemJson,1, TimeUnit.DAYS);
    }

    // 캐시에서 FundingItem 객체를 가져오기
    public FundingItem getCachedFundingProduct(String userCacheKey) throws JsonProcessingException {
        String cacheKey = "cachedFundingItem:" + userCacheKey;
        String fundingItemJson = redisTemplate.opsForValue().get(cacheKey);
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

    public void clearCache(String userCacheKey) {
        String cacheKey = "cachedFundingItem:" + userCacheKey;
        redisTemplate.delete(cacheKey);
    }

    private static String getMetaTagContent(Document document, String property) {
        Element metaTag = document.select("meta[property=" + property + "]").first();
        return (metaTag != null) ? metaTag.attr("content") : null;
    }

    @Transactional
    @CacheEvict(value = {"activeFundings", "finishedFundings", "fundingDetail"}, allEntries = true)
    public FundingResponseDto saveToDatabase(FundingCreateRequestDto requestDto, Long userId) throws JsonProcessingException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // 진행 중인 펀딩이 있는지 확인
        boolean hasActiveFunding = user.getFundings().stream()
                .anyMatch(funding -> funding.getStatus() == FundingStatus.ACTIVE);
        if (hasActiveFunding) {
            throw new IllegalStateException("Already has an active funding.");
        }
        // 캐시된 펀딩 아이템을 사용자 ID를 기반으로 가져옵니다.
        String userCacheKey = userId.toString();
        FundingItem fundingItem = getCachedFundingProduct(userCacheKey);
        if (fundingItem == null) {
            throw new IllegalStateException("No cached funding item found.");
        }

        LocalDate currentDate = LocalDate.now();
        FundingStatus status = requestDto.getEndDate().isBefore(currentDate) ? FundingStatus.FINISHED : FundingStatus.ACTIVE;
        Funding funding = requestDto.toEntity(fundingItem,status);
        funding.setUser(user);

        fundingRepository.save(funding);
        clearCache(userCacheKey);
        return FundingResponseDto.fromEntity(funding);
    }

    @Cacheable(value = "fundingDetail", key = "#fundingId")
    public FundingResponseDto findFunding(Long fundingId) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new NullPointerException("해당 펀딩을 찾을 수 없습니다."));
        FundingResponseDto responseDto = FundingResponseDto.fromEntity(funding);
        return responseDto;
    }

    @Cacheable(value = "activeFundings")
    @Transactional(readOnly = true)
    public List<FundingResponseDto> getActiveFundings() {
        LocalDate currentDate = LocalDate.now();
        List<Funding> fundings = fundingRepository.findByEndDateGreaterThanEqualAndStatus(currentDate, FundingStatus.ACTIVE);
        return fundings.stream()
                .map(FundingResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "finishedFundings")
    @Transactional(readOnly = true)
    public List<FundingResponseDto> getFinishedFunding() {
        List<Funding> fundings = fundingRepository.findByStatus(FundingStatus.FINISHED);
        return fundings.stream()
                .map(FundingResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = {"activeFundings", "finishedFundings", "fundingDetail"}, allEntries = true)
    public void finishFunding(Long fundingId, User currentUser) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new IllegalArgumentException("해당 펀딩을 찾을 수 없습니다."));

        // 펀딩을 등록한 사용자가 현재 로그인한 사용자와 일치하는지 확인
        if (!funding.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("이 펀딩을 종료할 권한이 없습니다.");
        }

        funding.setStatus(FundingStatus.FINISHED);
        fundingRepository.save(funding);
    }
}