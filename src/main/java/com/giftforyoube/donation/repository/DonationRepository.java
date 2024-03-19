package com.giftforyoube.donation.repository;

import com.giftforyoube.donation.entity.Donation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DonationRepository extends JpaRepository<Donation, Long> {

    List<Donation> findByFundingIdOrderByDonationRankingDesc(Long fundingId);

    List<Donation> findByFundingId(Long fundingId);
}