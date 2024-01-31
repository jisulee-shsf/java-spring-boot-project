package com.giftforyoube.funding.dto;

import com.giftforyoube.funding.entity.Funding;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class FundingCreateResponseDto {

    private Long id;
    private String itemLink;
    private String itemImage;
    private String itemName;
    private String title;
    private String content;
    private Integer goalAmount;
    private boolean publicFlag;
    private LocalDate endDate;

    public FundingCreateResponseDto(Funding funding) {
        this.id = funding.getId();
        this.itemLink = funding.getItemLink();
        this.itemImage = funding.getItemImage();
        this.itemName = funding.getItemName();
        this.title = funding.getTitle();
        this.content = funding.getContent();
        this.goalAmount = funding.getGoalAmount();
        this.publicFlag = funding.isPublicFlag();
        this.endDate = funding.getEndDate();
    }
}
