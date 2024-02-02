package com.giftforyoube.scheduler;

import com.giftforyoube.funding.entity.Funding;
import com.giftforyoube.funding.entity.FundingStatus;
import com.giftforyoube.funding.repository.FundingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j(topic = "Scheduler")
@Component
@RequiredArgsConstructor
public class Scheduler {

    private final FundingRepository fundingRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final CacheManager cacheManager;

    @Transactional
    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정에 요일 고려하지 않고 실행
    public void autoFinishFundings() {
        log.info("마감일 종료 상태 업데이트 실행");
        LocalDate currentDate = LocalDate.now();
        List<Funding> fundings = fundingRepository.findByEndDateLessThanAndStatus(currentDate, FundingStatus.ACTIVE);
        for (Funding funding : fundings) {
            funding.setStatus(FundingStatus.FINISHED);
            fundingRepository.save(funding); // 상태 변경을 저장해야 함

            Long fundingId = funding.getId();
            cacheManager.getCache("activeFundings").evict(fundingId);
            cacheManager.getCache("finishedFundings").put(fundingId, true);
        }
    }
}