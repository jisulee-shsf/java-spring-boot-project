package com.giftforyoube.funding.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giftforyoube.donation.repository.DonationRepository;
import com.giftforyoube.funding.dto.*;
import com.giftforyoube.funding.entity.Funding;
import com.giftforyoube.funding.entity.FundingItem;
import com.giftforyoube.funding.entity.FundingStatus;
import com.giftforyoube.funding.entity.FundingSummary;
import com.giftforyoube.funding.repository.FundingRepository;
import com.giftforyoube.funding.repository.FundingSummaryRepository;
import com.giftforyoube.global.exception.BaseException;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.user.entity.User;
import com.giftforyoube.user.repository.UserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Getter
@RequiredArgsConstructor
public class FundingService {

    private final RedisTemplate<String, String> redisTemplate;
    private final FundingRepository fundingRepository;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final RedissonClient redissonClient;
    private final DonationRepository donationRepository;
    private final FundingSummaryRepository fundingSummaryRepository;

    private static final int TIMEOUT = 10000; // 10초
    private static final String FUNDING_ITEM_CACHE_PREFIX = "cachedFundingItem:";
    private static final String FUNDING_SUMMARY_CACHE_KEY = "fundingSummary";

    // 데이터베이스 트랜잭션에 직접적으로 관련된 작업이 없으므로 @Transactional 어노테이션을 사용할 필요가 없음.
    public FundingItemResponseDto addLinkAndSaveToCache(AddLinkRequestDto requestDto, Long userId) throws IOException {
        log.info("[addLinkAndSaveToCache] 상품링크 캐쉬에 저장하기");

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
            return FundingItemResponseDto.fromEntity(fundingItem);
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
    public FundingResponseDto saveToDatabase(FundingCreateRequestDto requestDto, Long userId) throws JsonProcessingException {
        log.info("[saveToDatabase] DB에 저장하기");
        String lockKey = "userFundingLock:" + userId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean lockAcquired = false;
        try {
            // 락 획득 시도를 최적화
            lockAcquired = lock.tryLock(5, 1, TimeUnit.SECONDS); // 예: 5초 대기, 1초로 리스 타임 조정
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
            clearFundingCaches();
            return FundingResponseDto.fromEntity(funding);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("락을 획득하는 동안 문제가 발생하였습니다.", e);
        } finally {
            if (lockAcquired) {
                lock.unlock();
            }
        }
    }

    @Transactional(readOnly = true)
    public FundingResponseDto findFunding(Long fundingId) {
        String cacheKey = "fundingDetail:" + fundingId;
        // 캐시에서 조회 시도
        FundingResponseDto cachedFunding = getFundingFromCache(cacheKey);
        if (cachedFunding != null) {
            return cachedFunding;
        }

        // DB에서 조회
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new NullPointerException("해당 펀딩을 찾을 수 없습니다."));
        FundingResponseDto fundingResponseDto = FundingResponseDto.fromEntity(funding);

        // 결과를 캐시에 저장
        saveFundingToCache(cacheKey, fundingResponseDto);
        return fundingResponseDto;
    }

    // 메인페이지에 보여질 내 펀딩 정보
    @Transactional(readOnly = true)
    public FundingResponseDto getMyFundingInfo(User currentUser) {
        log.info("[getMyFundingInfo] 내 펀딩 정보 조회");

        String cacheKey = "fundingDetail:" + currentUser.getId();
        // 캐시에서 조회 시도
        FundingResponseDto cachedFunding = getFundingFromCache(cacheKey);
        if (cachedFunding != null) {
            return cachedFunding;
        }

        Funding funding = fundingRepository.findByUserAndStatus(currentUser, FundingStatus.ACTIVE);
        if (funding == null) {
            return FundingResponseDto.emptyDto();
        }

        FundingResponseDto fundingResponseDto = FundingResponseDto.fromEntity(funding);
        // 결과를 캐시에 저장
        saveFundingToCache(cacheKey, fundingResponseDto);

        return fundingResponseDto;
    }

    @Transactional(readOnly = true)
    public Page<FundingResponseDto> getActiveMainFunding(int page, int size, String sortBy, String sortOrder) {
        log.info("[getActiveMainFundings] 메인페이지 진행중인 펀딩 조회");

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy));
        String cacheKey = "activeMainFundings:" + page + ":" + size + ":" + sortBy + ":" + sortOrder;

        // 캐시에서 조회 시도
        Page<FundingResponseDto> cachedPage = getFundingPageFromCache(cacheKey, pageable);
        if (cachedPage != null) {
            return cachedPage;
        }

        // DB에서 조회
        Page<Funding> mainFundings = fundingRepository.findById(pageable);
        Page<FundingResponseDto> fundingResponseDtoPage = mainFundings.map(FundingResponseDto::fromEntity);

        // 결과를 캐시에 저장
        saveFundingPageToCache(cacheKey, fundingResponseDtoPage);

        return fundingResponseDtoPage;
    }

    @Transactional(readOnly = true)
    public Page<FundingResponseDto> getAllFundings(int page, int size, String sortBy, String sortOrder) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortOrder.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy));
        String cacheKey = "allFundings:" + page + ":" + size + ":" + sortBy + ":" + sortOrder;

        // 캐시에서 조회 시도
        Page<FundingResponseDto> cachedFundings = getFundingsPageFromCache(cacheKey, pageable);
        if (cachedFundings != null && !cachedFundings.isEmpty()) {
            return cachedFundings;
        }

        // DB에서 조회
        Page<Funding> allFunding = fundingRepository.findAll(pageable);
        Page<FundingResponseDto> allFundings = allFunding.map(FundingResponseDto::fromEntity);

        // 결과를 캐시에 저장
        saveFundingsPageToCache(cacheKey, allFundings);

        return allFundings;
    }

    // Slice - Page 페이지네이션 수정 적용
    @Transactional(readOnly = true)
    public Slice<FundingResponseDto> getActiveFundings(int page, int size, String sortBy, String sortOrder) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortOrder.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy));
        String cacheKey = "activeFundings:" + page + ":" + size + ":" + sortBy + ":" + sortOrder;

        // 캐시에서 조회 시도
        Slice<FundingResponseDto> cachedFundings = getFundingListFromCache(cacheKey, pageable);
        if (cachedFundings != null && !cachedFundings.isEmpty()) {
            return cachedFundings;
        }

        // DB에서 조회 및 캐시 저장
        Slice<FundingResponseDto> activeFundings = fundingRepository.findByStatus(FundingStatus.ACTIVE, pageable).map(FundingResponseDto::fromEntity);
        saveFundingListToCache(cacheKey, activeFundings);

        return activeFundings;
    }

    // 완료된 펀딩 페이지네이션 적용
    // 완료된 펀딩 조회
    @Transactional(readOnly = true)
    public Slice<FundingResponseDto> getFinishedFundings(int page, int size, String sortBy, String sortOrder) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortOrder.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy));
        String cacheKey = "finishedFundings:" + page + ":" + size + ":" + sortBy + ":" + sortOrder;

        // 캐시에서 조회 시도
        Slice<FundingResponseDto> cachedFundings = getFundingListFromCache(cacheKey, pageable);
        if (!cachedFundings.getContent().isEmpty()) {
            return cachedFundings;
        }

        // DB에서 조회 및 캐시 저장
        Slice<FundingResponseDto> finishedFundings = fundingRepository.findByStatus(FundingStatus.FINISHED, pageable).map(FundingResponseDto::fromEntity);
        saveFundingListToCache(cacheKey, finishedFundings);

        return finishedFundings;
    }

    @Transactional
    public void finishFunding(Long fundingId, User currentUser) {
        log.info("[finishFunding] 펀딩 종료하기");

        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new IllegalArgumentException("해당 펀딩을 찾을 수 없습니다."));

        // 펀딩을 등록한 사용자가 현재 로그인한 사용자와 일치하는지 확인
        if (!funding.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("이 펀딩을 종료할 권한이 없습니다.");
        }

        funding.setStatus(FundingStatus.FINISHED);
        fundingRepository.save(funding);
        clearFundingCaches();
    }

    // 펀딩 수정
    @Transactional
    public FundingResponseDto updateFunding(Long fundingId, User user, FundingUpdateRequestDto requestDto) {
        log.info("[updateFunding] 펀딩 수정하기");

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

        clearFundingCaches();
        return FundingResponseDto.fromEntity(funding);
    }

    // 펀딩 삭제
    @Transactional
    public void deleteFunding(Long fundingId, User user) {
        log.info("[deleteFunding] 펀딩 수정하기");

        // 펀딩 id 유효성 검사
        Funding funding = fundingRepository.findById(fundingId).orElseThrow(
                () -> new BaseException(BaseResponseStatus.FUNDING_NOT_FOUND)
        );

        // 현재 유저와 펀딩 유저가 같은지 유효성 검사
        if (!funding.getUser().getId().equals(user.getId())) {
            throw new BaseException(BaseResponseStatus.UNAUTHORIZED_DELETE_FUNDING);
        }

        fundingRepository.delete(funding);
        clearFundingCaches();
    }

    @Transactional(readOnly = true)
    public FundingSummaryResponseDto getFundingSummary() {
        // 캐시에서 통계 데이터를 검색합니다.
        FundingSummaryResponseDto cachedSummary = getSummaryFromCache(FUNDING_SUMMARY_CACHE_KEY);
        if (cachedSummary != null) {
            return cachedSummary;
        }

        // 캐시에 데이터가 없는 경우, 데이터베이스에서 정보를 계산합니다.
        FundingSummary fundingSummary = fundingSummaryRepository.findFirstByOrderByIdAsc().orElse(new FundingSummary());
        long totalDonationsCount = fundingSummary.getTotalDonationsCount();
        long successfulFundingsCount = fundingSummary.getSuccessfulFundingsCount();
        long totalFundingAmount = fundingSummary.getTotalFundingAmount();

        // 계산된 통계 정보를 캐시에 저장합니다.
        FundingSummaryResponseDto summary = FundingSummaryResponseDto.builder()
                .totalDonationsCount(totalDonationsCount)
                .successfulFundingsCount(successfulFundingsCount)
                .totalFundingAmount(totalFundingAmount)
                .build();

        saveSummaryToCache(FUNDING_SUMMARY_CACHE_KEY, summary);
        return summary;
    }

    // 목표금액 달성되어서 종료된 펀딩 카운트 증가 메서드 추가 예정


    // ---------------------------- 캐시 관련 메서드들과 OG 태그 메서드 ------------------------------------------

    private String buildCacheKey(String userId) {
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

        String fundingItemJson = redisTemplate.opsForValue().get(cacheKey);
        return fundingItemJson == null ? null : objectMapper.readValue(fundingItemJson, FundingItem.class);
    }

    public FundingItem previewItem(String itemLink) throws IOException {
        log.info("[previewItem] 상품 미리보기");

        Document document = Jsoup.connect(itemLink).timeout(TIMEOUT).get();
        String itemImage = getMetaTagContent(document, "og:image");
        if (itemImage == null) {
            throw new IOException("링크 상품 이미지를 가져올 수 없습니다.");
        }
        return new FundingItem(itemLink, itemImage);
    }

    public void clearCache(String userCacheKey) {
        log.info("[clearCache] 캐쉬 삭제하기");

        String cacheKey = buildCacheKey(userCacheKey);
        redisTemplate.delete(cacheKey);
    }

    private static String getMetaTagContent(Document document, String property) {
        log.info("[getMetaTagContent] 메타 태크에서 상품이미지 가져오기");

        Elements metaTags = document.select("meta[property=" + property + "]");
        if (!metaTags.isEmpty()) {
            return metaTags.first().attr("content");
        }
        return null;
    }


    //    // 캐시에 Page 데이터 저장
    private void saveFundingPageToCache(String cacheKey, Page<FundingResponseDto> page) {
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
    private Page<FundingResponseDto> getFundingPageFromCache(String cacheKey, Pageable pageable) {
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
    private void saveFundingToCache(String cacheKey, FundingResponseDto fundingResponseDto) {
        try {
            String jsonContent = objectMapper.writeValueAsString(fundingResponseDto);
            redisTemplate.opsForValue().set(cacheKey, jsonContent, Duration.ofHours(1));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing funding detail", e);
        }
    }

    // 펀딩 상세 정보 캐시에서 조회
    private FundingResponseDto getFundingFromCache(String cacheKey) {
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
    private FundingSummaryResponseDto getSummaryFromCache(String cacheKey) {
        String jsonContent = redisTemplate.opsForValue().get(cacheKey);
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
    private void saveSummaryToCache(String cacheKey, FundingSummaryResponseDto summary) {
        try {
            String jsonContent = objectMapper.writeValueAsString(summary);
            redisTemplate.opsForValue().set(cacheKey, jsonContent, Duration.ofHours(1)); // 캐시 유지 시간은 요구 사항에 따라 조정 가능
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

        // 상세 페이지 캐시 삭제 추가
        clearCacheByPattern("fundingDetail:*");

        // Giftipie에서 함께한 선물 캐시 삭제
        clearCacheByPattern(FUNDING_SUMMARY_CACHE_KEY);
    }

    private void clearCacheByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}