package com.giftforyoube.funding.service;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Getter
@RequiredArgsConstructor
public class FundingService {

    private final FundingRepository fundingRepository;
    private final UserRepository userRepository;
    private final RedissonClient redissonClient;
    private final DonationRepository donationRepository;
    private final FundingSummaryRepository fundingSummaryRepository;
    private final CacheService cacheService;

    private static final int TIMEOUT = 10000; // 10초

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
            cacheService.saveToCache(fundingItem, userId.toString());
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
            lockAcquired = lock.tryLock(10, 2, TimeUnit.SECONDS); // 예: 5초 대기, 1초로 리스 타임 조정
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
            String userCacheKey = cacheService.buildCacheKey(userId.toString());
            FundingItem fundingItem = cacheService.getCachedFundingProduct(userCacheKey);
            if (fundingItem == null) {
                throw new IllegalStateException("링크 상품을 찾을 수 없습니다.");
            }
            LocalDate currentDate = LocalDate.now();
            FundingStatus status = requestDto.getEndDate().isBefore(currentDate) ? FundingStatus.FINISHED : FundingStatus.ACTIVE;
            Funding funding = requestDto.toEntity(fundingItem, status);
            funding.setUser(user);
            fundingRepository.save(funding);
            cacheService.clearCache(userCacheKey);
            cacheService.clearFundingCaches();
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
        FundingResponseDto cachedFunding = cacheService.getFundingFromCache(cacheKey);
        if (cachedFunding != null) {
            return cachedFunding;
        }

        // DB에서 조회
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new NullPointerException("해당 펀딩을 찾을 수 없습니다."));
        FundingResponseDto fundingResponseDto = FundingResponseDto.fromEntity(funding);

        // 결과를 캐시에 저장
        cacheService.saveFundingToCache(cacheKey, fundingResponseDto);
        return fundingResponseDto;
    }

    // 메인페이지에 보여질 내 펀딩 정보
    @Transactional(readOnly = true)
    public FundingResponseDto getMyFundingInfo(User currentUser) {
        log.info("[getMyFundingInfo] 내 펀딩 정보 조회");

        String cacheKey = "fundingDetail:" + currentUser.getId();
        // 캐시에서 조회 시도
        FundingResponseDto cachedFunding = cacheService.getFundingFromCache(cacheKey);
        if (cachedFunding != null) {
            return cachedFunding;
        }

        Funding funding = fundingRepository.findByUserAndStatus(currentUser, FundingStatus.ACTIVE);
        if (funding == null) {
            return FundingResponseDto.emptyDto();
        }

        FundingResponseDto fundingResponseDto = FundingResponseDto.fromEntity(funding);
        // 결과를 캐시에 저장
        cacheService.saveFundingToCache(cacheKey, fundingResponseDto);

        return fundingResponseDto;
    }

    @Transactional(readOnly = true)
    public Page<FundingResponseDto> getActiveMainFunding(int page, int size, String sortBy, String sortOrder) {
        log.info("[getActiveMainFundings] 메인페이지 진행중인 펀딩 조회");

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy));
        String cacheKey = "activeMainFundings:" + page + ":" + size + ":" + sortBy + ":" + sortOrder;

        // 캐시에서 조회 시도
        Page<FundingResponseDto> cachedPage = cacheService.getFundingPageFromCache(cacheKey, pageable);
        if (cachedPage != null) {
            return cachedPage;
        }

        // DB에서 조회
        Page<Funding> mainFundings = fundingRepository.findAllAndPublicFlagTrue(pageable);
        Page<FundingResponseDto> fundingResponseDtoPage = mainFundings.map(FundingResponseDto::fromEntity);

        // 결과를 캐시에 저장
        cacheService.saveFundingPageToCache(cacheKey, fundingResponseDtoPage);

        return fundingResponseDtoPage;
    }

    @Transactional(readOnly = true)
    public Page<FundingResponseDto> getAllFundings(int page, int size, String sortBy, String sortOrder) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortOrder.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy));
        String cacheKey = "allFundings:" + page + ":" + size + ":" + sortBy + ":" + sortOrder;

        // 캐시에서 조회 시도
        Page<FundingResponseDto> cachedFundings = cacheService.getFundingsPageFromCache(cacheKey, pageable);
        if (cachedFundings != null && !cachedFundings.isEmpty()) {
            return cachedFundings;
        }

        // DB에서 조회
        Page<Funding> allFunding = fundingRepository.findAllAndPublicFlagTrue(pageable);
        Page<FundingResponseDto> allFundings = allFunding.map(FundingResponseDto::fromEntity);

        // 결과를 캐시에 저장
        cacheService.saveFundingsPageToCache(cacheKey, allFundings);

        return allFundings;
    }

    // Slice - Page 페이지네이션 수정 적용
    @Transactional(readOnly = true)
    public Slice<FundingResponseDto> getActiveFundings(int page, int size, String sortBy, String sortOrder) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortOrder.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy));
        String cacheKey = "activeFundings:" + page + ":" + size + ":" + sortBy + ":" + sortOrder;

        // 캐시에서 조회 시도
        Slice<FundingResponseDto> cachedFundings = cacheService.getFundingListFromCache(cacheKey, pageable);
        if (cachedFundings != null && !cachedFundings.isEmpty()) {
            return cachedFundings;
        }

        // DB에서 조회 및 캐시 저장
        Slice<FundingResponseDto> activeFundings = fundingRepository.findByStatusAndPublicFlagTrue(FundingStatus.ACTIVE, pageable).map(FundingResponseDto::fromEntity);
        cacheService.saveFundingListToCache(cacheKey, activeFundings);

        return activeFundings;
    }

    // 완료된 펀딩 페이지네이션 적용
    // 완료된 펀딩 조회
    @Transactional(readOnly = true)
    public Slice<FundingResponseDto> getFinishedFundings(int page, int size, String sortBy, String sortOrder) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortOrder.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy));
        String cacheKey = "finishedFundings:" + page + ":" + size + ":" + sortBy + ":" + sortOrder;

        // 캐시에서 조회 시도
        Slice<FundingResponseDto> cachedFundings = cacheService.getFundingListFromCache(cacheKey, pageable);
        if (!cachedFundings.getContent().isEmpty()) {
            return cachedFundings;
        }

        // DB에서 조회 및 캐시 저장
        Slice<FundingResponseDto> finishedFundings = fundingRepository.findByStatusAndPublicFlagTrue(FundingStatus.FINISHED, pageable).map(FundingResponseDto::fromEntity);
        cacheService.saveFundingListToCache(cacheKey, finishedFundings);

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
        cacheService.clearFundingCaches();
    }

    // 펀딩 수정
    @Transactional
    public FundingResponseDto updateFunding(Long fundingId, User user, FundingUpdateRequestDto requestDto) {
        log.info("[updateFunding] 펀딩 수정하기");

        String lockKey = "fundingUpdateLock:" + fundingId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(10, 2, TimeUnit.MINUTES)) {
                throw new IllegalStateException("펀딩 수정을 위한 락을 획득할 수 없습니다.");
            }

            // 펀딩 id 유효성 검사 및 수정 로직
            Funding funding = fundingRepository.findById(fundingId).orElseThrow(
                    () -> new BaseException(BaseResponseStatus.FUNDING_NOT_FOUND)
            );
            // 현재 유저와 펀딩 유저가 같은지 유효성 검사
            if (!funding.getUser().getId().equals(user.getId())) {
                throw new BaseException(BaseResponseStatus.UNAUTHORIZED_UPDATE_FUNDING);
            }

            funding.update(requestDto); // 펀딩 내용수정
            cacheService.clearFundingCaches(); // 캐시 무효화
            return FundingResponseDto.fromEntity(funding);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("락을 획득하는 동안 문제가 발생하였습니다.", e);
        } finally {
            if(lock.isLocked() && lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        }
    }

    // 펀딩 삭제
    @Transactional
    public void deleteFunding(Long fundingId, User user) {
        log.info("[deleteFunding] 펀딩 삭제하기");

        String lockKey = "fundingDeleteLock:" + fundingId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(10, 2, TimeUnit.MINUTES)) {
                throw new IllegalStateException("펀딩 삭제를 위한 락을 획득할 수 없습니다.");
            }

            // 펀딩 id 유효성 검사 및 삭제 로직
            Funding funding = fundingRepository.findById(fundingId).orElseThrow(
                    () -> new BaseException(BaseResponseStatus.FUNDING_NOT_FOUND)
            );

            // 현재 유저와 펀딩 유저가 같은지 유효성 검사
            if (!funding.getUser().getId().equals(user.getId())) {
                throw new BaseException(BaseResponseStatus.UNAUTHORIZED_DELETE_FUNDING);
            }

            fundingRepository.delete(funding);
            cacheService.clearFundingCaches(); // 캐시 무효화
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("락을 획득하는 동안 문제가 발생하였습니다.", e);
        } finally {
            if(lock.isLocked() && lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        }
    }


    @Transactional(readOnly = true)
    public FundingSummaryResponseDto getFundingSummary() {
        // 캐시에서 통계 데이터를 검색합니다.
        FundingSummaryResponseDto cachedSummary = cacheService.getSummaryFromCache();
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

        cacheService.saveSummaryToCache(summary);
        return summary;
    }

    // ---------------------------- OG 태그 메서드 ------------------------------------------


    public FundingItem previewItem(String itemLink) throws IOException {
        log.info("[previewItem] 상품 미리보기");

        Document document = Jsoup.connect(itemLink).timeout(TIMEOUT).get();
        String itemImage = getMetaTagContent(document, "og:image");
        if (itemImage == null) {
            throw new IOException("링크 상품 이미지를 가져올 수 없습니다.");
        }
        return new FundingItem(itemLink, itemImage);
    }

    private static String getMetaTagContent(Document document, String property) {
        log.info("[getMetaTagContent] 메타 태크에서 상품이미지 가져오기");

        Elements metaTags = document.select("meta[property=" + property + "]");
        if (!metaTags.isEmpty()) {
            return metaTags.first().attr("content");
        }
        return null;
    }
}