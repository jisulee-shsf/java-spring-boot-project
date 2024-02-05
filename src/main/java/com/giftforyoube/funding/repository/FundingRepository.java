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
    List<Funding> findByEndDateGreaterThanEqualAndStatus(LocalDate currentDate, FundingStatus status);
    Page<Funding> findByOrderByIdAsc(LocalDate currentDate, FundingStatus status, Pageable pageable);
    Slice<Funding> findByOrderByIdDesc(LocalDate currentDate, FundingStatus status, Pageable pageable);

    List<Funding> findByEndDateLessThanAndStatus(LocalDate currentDate, FundingStatus status);
    Page<Funding> findByEndDateLessThanAndStatus(LocalDate currentDate, FundingStatus status, Pageable pageable);

    List<Funding> findByStatus(FundingStatus fundingStatus);
    Slice<Funding> findByStatus(FundingStatus fundingStatus, Pageable pageable);
    List<Funding> findByEndDateLessThanEqualAndStatus(LocalDate currentDate, FundingStatus fundingStatus);
}