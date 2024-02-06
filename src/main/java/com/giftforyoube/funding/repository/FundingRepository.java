package com.giftforyoube.funding.repository;

import com.giftforyoube.funding.entity.Funding;
import com.giftforyoube.funding.entity.FundingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FundingRepository extends JpaRepository<Funding, Long> {
    Page<Funding> findAllPageByStatus(FundingStatus status, Pageable pageable);
    Slice<Funding> findByStatus(FundingStatus fundingStatus, Pageable pageable);
    List<Funding> findByEndDateLessThanEqualAndStatus(LocalDate currentDate, FundingStatus fundingStatus);
}