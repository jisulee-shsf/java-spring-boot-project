package com.giftforyoube.donation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ReadyDonationDto {

    @Getter
    @NoArgsConstructor
    public static class ReadyDonationRequestDto {
        private String sponsorNickname;
        private String sponsorComment;
        private int donation;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReadyDonationResponseDto {

        private String tid;
        private String next_redirect_pc_url;
        private String next_redirect_mobile_url;
    }
}