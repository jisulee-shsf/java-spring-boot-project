package com.giftforyoube.funding.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class FundingSummaryResponseDto {
    private final long totalDonationsCount;
    private final long successfulFundingsCount;
    private final long totalFundingAmount;

    @Builder
    public FundingSummaryResponseDto(long totalDonationsCount, long successfulFundingsCount, long totalFundingAmount) {
        this.totalDonationsCount = totalDonationsCount;
        this.successfulFundingsCount = successfulFundingsCount;
        this.totalFundingAmount = totalFundingAmount;
    }
}
