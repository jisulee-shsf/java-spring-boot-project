package com.giftforyoube.donation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ApproveDonationResponseDto {

    private Amount amount;

    @Getter
    public static class Amount {
        private int total;
    }
}