package com.giftforyoube.scheduler;

import com.giftforyoube.funding.entity.Funding;
import com.giftforyoube.funding.entity.FundingStatus;
import com.giftforyoube.funding.repository.FundingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

//        List<Funding> fundings = fundingRepository.findByEndDateLessThanAndStatus(currentDate, FundingStatus.ACTIVE);
@Slf4j(topic = "Scheduler")
@Component
@RequiredArgsConstructor
public class Scheduler {

    private final FundingRepository fundingRepository;
    // 매일 자정에 실행, 마감일이 지난 펀딩의 상태를 업데이트
    // 초, 분, 시, 일, 월, 주 순서
    @Scheduled(cron = "0 44 0 * * ?")
    @Transactional
    public void autoFinishFundings() {
        log.info("마감일 종료 상태 업데이트 실행");
        LocalDate currentDate = LocalDate.now();
        List<Funding> fundings = fundingRepository.findByEndDateLessThanEqualAndStatus(currentDate, FundingStatus.ACTIVE);
        for (Funding funding : fundings) {
            funding.setStatus(FundingStatus.FINISHED);
            fundingRepository.save(funding);
        }
        // 캐시 갱신
        cacheEvict();
    }

    @CacheEvict(value = {"activeFundings", "finishedFundings", "fundingDetail"}, allEntries = true)
    public void cacheEvict() {
        // @CacheEvict 어노테이션으로 처리되므로 별도의 로직이 필요 없음
    }
}