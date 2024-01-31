package com.giftforyoube.funding.service;

import com.giftforyoube.funding.entity.FundingItem;
import com.giftforyoube.funding.repository.FundingRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

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

    public void clearCache() {
        redisTemplate.delete("cachedFundingItem");
    }

    private static String getMetaTagContent(Document document, String property) {
        Element metaTag = document.select("meta[property=" + property + "]").first();
        return (metaTag != null) ? metaTag.attr("content") : null;
    }
}
