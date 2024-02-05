package com.giftforyoube.funding.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giftforyoube.funding.dto.AddLinkRequestDto;
import com.giftforyoube.funding.dto.FundingCreateRequestDto;
import com.giftforyoube.funding.dto.FundingResponseDto;
import com.giftforyoube.funding.entity.Funding;
import com.giftforyoube.funding.entity.FundingItem;
import com.giftforyoube.funding.entity.FundingStatus;
import com.giftforyoube.funding.repository.FundingRepository;
import com.giftforyoube.global.exception.BaseException;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.global.security.UserDetailsImpl;
import com.giftforyoube.user.entity.User;
import com.giftforyoube.user.repository.UserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Duration;
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
    private final RedissonClient redissonClient;

    private static final int TIMEOUT = 10000; // 10초
    private static final String FUNDING_ITEM_CACHE_PREFIX = "cachedFundingItem:";

    // 데이터베이스 트랜잭션에 직접적으로 관련된 작업이 없으므로 @Transactional 어노테이션을 사용할 필요가 없음.
    public void addLinkAndSaveToCache(AddLinkRequestDto requestDto, Long userId) throws IOException {
        String lockKey = "userLock:" + userId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean lockAcquired = false; // 락 획득 상태
        try {
            lockAcquired = lock.tryLock(10, 2, TimeUnit.MINUTES); // 락 획득 시도
            if (!lockAcquired) {
                throw new IllegalStateException("해당 사용자에 대한 락을 획득할 수 없습니다 : " + userId);
            }
            FundingItem fundingItem = previewItem(requestDto.getItemLink());
            saveToCache(fundingItem, userId.toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("락을 획득하는 동안 문제가 발생하였습니다.", e);
        } finally {
            if (lockAcquired) {
                lock.unlock(); // 락 해제
            }
        }
    }

    @Transactional
    @CacheEvict(value = {"activeFundings", "finishedFundings", "fundingDetail"}, cacheManager = "cacheManager", allEntries = true)
    public FundingResponseDto saveToDatabase(FundingCreateRequestDto requestDto, Long userId) throws JsonProcessingException {
        String lockKey = "userFundingLock:" + userId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean lockAcquired = false; // 락 획득 상태
        try {
            lockAcquired = lock.tryLock(10, 2, TimeUnit.MINUTES); // 락 획득 시도
            if (!lockAcquired) {
                throw new IllegalStateException("해당 사용자에 대한 락을 획득할 수 없습니다 : " + userId);
            }
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UsernameNotFoundException("해당 회원을 찾을 수 없습니다."));
            boolean hasActiveFunding = user.getFundings().stream()
                    .anyMatch(funding -> funding.getStatus() == FundingStatus.ACTIVE);
            if (hasActiveFunding) {
                throw new IllegalStateException("이미 진행중인 펀딩이 있습니다.");
            }
            String userCacheKey = buildCacheKey(userId.toString());
            FundingItem fundingItem = getCachedFundingProduct(userCacheKey);
            if (fundingItem == null) {
                throw new IllegalStateException("링크 상품을 찾을 수 없습니다.");
            }
            LocalDate currentDate = LocalDate.now();
            FundingStatus status = requestDto.getEndDate().isBefore(currentDate) ? FundingStatus.FINISHED : FundingStatus.ACTIVE;
            Funding funding = requestDto.toEntity(fundingItem, status);
            funding.setUser(user);
            fundingRepository.save(funding);
            clearCache(userCacheKey);
            return FundingResponseDto.fromEntity(funding);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("락을 획득하는 동안 문제가 발생하였습니다.", e);
        } finally {
            lock.unlock(); // 락 해제
        }
    }

    @Cacheable(value = "fundingDetail", key = "#fundingId", cacheManager = "cacheManager")
    public FundingResponseDto findFunding(Long fundingId) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new NullPointerException("해당 펀딩을 찾을 수 없습니다."));
        return FundingResponseDto.fromEntity(funding);
    }

    @Cacheable(value = "activeFundings", cacheManager = "cacheManager")
    @Transactional(readOnly = true)
    public List<FundingResponseDto> getActiveFundings() {
        LocalDate currentDate = LocalDate.now();
        List<Funding> fundings = fundingRepository.findByEndDateGreaterThanEqualAndStatus(currentDate, FundingStatus.ACTIVE);
        return fundings.stream().map(FundingResponseDto::fromEntity).collect(Collectors.toList());
    }

    @Cacheable(value = "finishedFundings", cacheManager = "cacheManager")
    @Transactional(readOnly = true)
    public List<FundingResponseDto> getFinishedFunding() {
        List<Funding> fundings = fundingRepository.findByStatus(FundingStatus.FINISHED);
        return fundings.stream().map(FundingResponseDto::fromEntity).collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = {"activeFundings", "finishedFundings", "fundingDetail"}, cacheManager = "cacheManager", allEntries = true)
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

    private String buildCacheKey(String userId) {
        return FUNDING_ITEM_CACHE_PREFIX + userId;
    }

    // FundingItem 객체를 JSON으로 변환하여 캐시에 저장
    public void saveToCache(FundingItem fundingItem, String userId) throws JsonProcessingException {
        String cacheKey = buildCacheKey(userId);
        String fundingItemJson = objectMapper.writeValueAsString(fundingItem);
        redisTemplate.opsForValue().set(cacheKey, fundingItemJson, Duration.ofDays(1));
    }

    // 캐시에서 FundingItem 객체를 가져오기
    public FundingItem getCachedFundingProduct(String userCacheKey) throws JsonProcessingException {
        String cacheKey = buildCacheKey(userCacheKey);
        String fundingItemJson = redisTemplate.opsForValue().get(cacheKey);
        return fundingItemJson == null ? null : objectMapper.readValue(fundingItemJson, FundingItem.class);
    }

    public FundingItem previewItem(String itemLink) throws IOException {
        Document document = Jsoup.connect(itemLink).timeout(TIMEOUT).get();
        String itemImage = getMetaTagContent(document, "og:image");
        if (itemImage == null) {
            throw new IOException("링크 상품 이미지를 가져올 수 없습니다.");
        }
        return new FundingItem(itemLink, itemImage);
    }

    public void clearCache(String userCacheKey) {
        String cacheKey = buildCacheKey(userCacheKey);
        redisTemplate.delete(cacheKey);
    }

    private static String getMetaTagContent(Document document, String property) {
        Elements metaTags = document.select("meta[property=" + property + "]");
        if (!metaTags.isEmpty()) {
            return metaTags.first().attr("content");
        }
        return null;
    }

    // 펀딩 수정
    @Transactional
    @CacheEvict(value = {"activeFundings", "finishedFundings", "fundingDetail"}, allEntries = true)
    public FundingResponseDto updateFunding(Long fundingId, User user, FundingCreateRequestDto requestDto) {
        // 펀딩 id 유효성검사
        Funding funding = fundingRepository.findById(fundingId).orElseThrow(
                () -> new BaseException(BaseResponseStatus.FUNDING_NOT_FOUND)
        );

        // 현재 유저와 펀딩 유저가 같은지 유효성 검사
        if (!funding.getUser().getId().equals(user.getId())) {
            throw new BaseException(BaseResponseStatus.UNAUTHORIZED_UPDATE_FUNDING);
        }

        // 펀딩 내용수정
        funding.update(requestDto);

        return FundingResponseDto.fromEntity(funding);
    }

    // 펀딩 삭제
    @Transactional
    @CacheEvict(value = {"activeFundings", "finishedFundings", "fundingDetail"}, allEntries = true)
    public void deleteFunding(Long fundingId, User user) {
        // 펀딩 id 유효성 검사
        Funding funding = fundingRepository.findById(fundingId).orElseThrow(
                () -> new BaseException(BaseResponseStatus.FUNDING_NOT_FOUND)
        );

        // 현재 유저와 펀딩 유저가 같은지 유효성 검사
        if (!funding.getUser().getId().equals(user.getId())) {
            throw new BaseException(BaseResponseStatus.UNAUTHORIZED_DELETE_FUNDING);
        }

        fundingRepository.delete(funding);
    }
}