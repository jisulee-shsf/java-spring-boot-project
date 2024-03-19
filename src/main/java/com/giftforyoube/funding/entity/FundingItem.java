package com.giftforyoube.funding.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class FundingItem implements Serializable {

    private String itemLink;
    private String itemImage;

    public FundingItem() {
        // 기본 생성자
    }

    @Builder
    public FundingItem(String itemLink, String itemImage) {
        this.itemLink = itemLink;
        this.itemImage = itemImage;
    }
}