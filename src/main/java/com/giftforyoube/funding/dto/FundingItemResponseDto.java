package com.giftforyoube.funding.dto;

import com.giftforyoube.funding.entity.FundingItem;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FundingItemResponseDto {

    private String itemLink;
    private String itemImage;

    @Builder
    public FundingItemResponseDto(String itemLink, String itemImage) {
        this.itemLink = itemLink;
        this.itemImage = itemImage;
    }

    public static FundingItemResponseDto fromEntity(FundingItem fundingItem){
        return FundingItemResponseDto.builder()
                .itemLink(fundingItem.getItemLink())
                .itemImage(fundingItem.getItemImage())
                .build();
    }
}