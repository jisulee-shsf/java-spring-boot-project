package com.giftforyoube.funding.dto;

import com.giftforyoube.funding.entity.Funding;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class FundingResponseDtoCache {
    private List<FundingResponseDto> content;
    private int page;
    private int size;
    private boolean last;

    // 생성자, Getter 및 Setter
    public FundingResponseDtoCache() {
    }

    public FundingResponseDtoCache(List<FundingResponseDto> content, int page, int size, boolean last) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.last = last;
    }
}