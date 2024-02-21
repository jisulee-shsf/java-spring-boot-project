package com.giftforyoube.funding.repository;

import com.giftforyoube.funding.entity.FundingSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FundingSummaryRepository extends JpaRepository<FundingSummary, Long> {
    Optional<FundingSummary> findFirstByOrderByIdAsc();
}
