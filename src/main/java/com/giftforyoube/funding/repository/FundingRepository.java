package com.giftforyoube.funding.repository;

import com.giftforyoube.funding.entity.Funding;
import com.giftforyoube.funding.entity.FundingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FundingRepository extends JpaRepository<Funding, Long> {
    List<Funding> findByEndDateGreaterThanEqualAndStatus(LocalDate currentDate, FundingStatus status);
    List<Funding> findByEndDateLessThanAndStatus(LocalDate currentDate, FundingStatus status);
}
