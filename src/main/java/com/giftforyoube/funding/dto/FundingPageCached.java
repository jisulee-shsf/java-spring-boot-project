package com.giftforyoube.funding.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FundingPageCached<T> {
    private List<T> content;
    private FundingPageMetadata metadata;
}
