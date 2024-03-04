package com.giftforyoube.donation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ReadyDonationDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public class RequestDto {

        private String sponsorNickname;
        private String sponsorComment;
        private int donation;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public class ResponseDto {

        private String tid;
        private String next_redirect_pc_url;
        private String next_redirect_mobile_url;
    }
}