package com.giftforyoube.fundingItem.repository;

import com.giftforyoube.donation.entity.Donation;
import com.giftforyoube.fundingItem.entity.FundingItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FundingItemRepository extends JpaRepository<FundingItem, Long> {
}
