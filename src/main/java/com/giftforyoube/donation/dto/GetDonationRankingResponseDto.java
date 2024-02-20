package com.giftforyoube.donation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GetDonationRankingResponseDto {

    private int donationRanking;

    public GetDonationRankingResponseDto(int donationRanking) {
        this.donationRanking = donationRanking;
    }
}