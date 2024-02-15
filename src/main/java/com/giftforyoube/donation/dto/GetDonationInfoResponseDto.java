package com.giftforyoube.donation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GetDonationInfoResponseDto {

    private String sponsorNickname;
    private String sponsorComment;
    private int donationRanking;

    public GetDonationInfoResponseDto(String sponsorNickname, String sponsorComment, int donationRanking) {
        this.sponsorNickname = sponsorNickname;
        this.sponsorComment = sponsorComment;
        this.donationRanking = donationRanking;
    }
}