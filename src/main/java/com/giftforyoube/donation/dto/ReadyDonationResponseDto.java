package com.giftforyoube.donation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReadyDonationResponseDto {
    private String tid;
    private String next_redirect_mobile_url;
    private String next_redirect_pc_url;
    private String created_at;
}