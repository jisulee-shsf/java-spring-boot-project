package com.giftforyoube.funding.dto;

import com.giftforyoube.funding.entity.Funding;
import com.giftforyoube.funding.entity.FundingItem;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class FundingCreateRequestDto {
    private String itemName;
    private String title;
    private String content;
    private int targetAmount;
    private boolean publicFlag;
    private LocalDate endDate;

    // 생성자, Getter, Setter 등 필요한 메서드를 추가할 수 있습니다.

    public FundingCreateRequestDto() {
        // 기본 생성자
    }

    public Funding toEntity(FundingItem fundingItem) {
        return Funding.builder()
                .itemLink(fundingItem.getItemLink())
                .itemImage(fundingItem.getItemImage())
                .itemName(this.itemName)
                .title(this.title)
                .content(this.content)
                .currentAmount(0)
                .targetAmount(this.targetAmount)
                .publicFlag(this.publicFlag)
                .endDate(this.getEndDate())
                .build();
    }
}