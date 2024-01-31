package com.giftforyoube.funding.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class FundingCreateRequestDto {
    private String itemName;
    private String title;
    private String content;
    private Integer goalAmount;
    private boolean publicFlag;
    private LocalDate endDate;

    // 생성자, Getter, Setter 등 필요한 메서드를 추가할 수 있습니다.

    public FundingCreateRequestDto() {
        // 기본 생성자
    }
}