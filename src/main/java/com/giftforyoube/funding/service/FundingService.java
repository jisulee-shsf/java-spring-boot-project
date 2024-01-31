package com.giftforyoube.funding.service;

import com.giftforyoube.funding.dto.FundingCreateRequestDto;
import com.giftforyoube.funding.dto.FundingCreateResponseDto;
import com.giftforyoube.funding.entity.Funding;
import com.giftforyoube.funding.entity.FundingItem;
import com.giftforyoube.funding.repository.FundingRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Service
@Getter
@RequiredArgsConstructor
public class FundingService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final FundingRepository fundingRepository;

    private static final int TIMEOUT = 10000; // 10초

    public void saveToCache(FundingItem fundingItem) {
        redisTemplate.opsForValue().set("cachedFundingItem", fundingItem);
    }

    public FundingItem getCachedFundingProduct() {
        return (FundingItem) redisTemplate.opsForValue().get("cachedFundingItem");
    }

    public FundingItem previewItem(String itemLink) throws IOException {
        Document document = Jsoup.connect(itemLink).timeout(TIMEOUT).get();
        String itemImage = getMetaTagContent(document, "og:image");
        if (itemImage == null) {
            throw new IOException("Cannot fetch item image.");
        }
        return new FundingItem(itemLink, itemImage);
    }

    @Transactional
    public FundingCreateResponseDto saveToDatabase(FundingCreateRequestDto requestDto) {
        FundingItem fundingItem = getCachedFundingProduct();
        if (fundingItem == null) {
            throw new IllegalStateException("No cached funding item found.");
        }
        Funding funding = new Funding(
                fundingItem.getItemLink(),
                fundingItem.getItemImage(),
                requestDto.getItemName(),
                requestDto.getTitle(),
                requestDto.getContent(),
                requestDto.getGoalAmount(),
                requestDto.isPublicFlag(),
                requestDto.getEndDate()
        );
        fundingRepository.save(funding);
        return new FundingCreateResponseDto(funding);
    }

    public void clearCache() {
        redisTemplate.delete("cachedFundingItem");
    }

    private static String getMetaTagContent(Document document, String property) {
        Element metaTag = document.select("meta[property=" + property + "]").first();
        return (metaTag != null) ? metaTag.attr("content") : null;
    }
}
