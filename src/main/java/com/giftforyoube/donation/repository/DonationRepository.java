package com.giftforyoube.donation.repository;

import com.giftforyoube.donation.entity.Donation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DonationRepository extends JpaRepository<Donation, Long> {

    List<Donation> findByFundingIdOrderByDonationRankingDesc(Long fundingId);

    @Query("SELECT SUM(d.donationAmount) FROM Donation d WHERE d.funding.id = :fundingId")
    int getTotalDonationAmountByFundingId(Long fundingId);
}