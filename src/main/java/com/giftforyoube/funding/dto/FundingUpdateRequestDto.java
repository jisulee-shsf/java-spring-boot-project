package com.giftforyoube.funding.dto;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class FundingUpdateRequestDto {
    private String showName;
    private String title;
    private String content;
    private boolean publicFlag;
}
