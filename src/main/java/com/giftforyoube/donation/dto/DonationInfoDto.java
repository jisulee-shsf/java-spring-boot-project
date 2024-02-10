package com.giftforyoube.donation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DonationInfoDto {

    private String sponsorNickname;
    private String comment;
    private int donationAmount;

    public DonationInfoDto(String sponsorNickname, String comment, int donationAmount) {
        this.sponsorNickname = sponsorNickname;
        this.comment = comment;
        this.donationAmount = donationAmount;
    }
}