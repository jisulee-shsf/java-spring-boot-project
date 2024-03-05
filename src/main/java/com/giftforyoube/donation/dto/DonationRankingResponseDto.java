package com.giftforyoube.donation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DonationRankingResponseDto {

    private int donationRanking;

    public DonationRankingResponseDto(int donationRanking) {
        this.donationRanking = donationRanking;
    }
}