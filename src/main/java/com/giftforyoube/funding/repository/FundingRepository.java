package com.giftforyoube.funding.repository;

import com.giftforyoube.donation.entity.Donation;
import com.giftforyoube.funding.entity.Funding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FundingRepository extends JpaRepository<Funding, Long> {
    List<Funding> findByEndDateGreaterThanEqual(LocalDate currentDate);
    List<Funding> findByEndDateLessThan(LocalDate currentDate);
}
