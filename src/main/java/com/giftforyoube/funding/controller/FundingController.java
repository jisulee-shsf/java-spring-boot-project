package com.giftforyoube.funding.controller;

import com.giftforyoube.funding.dto.AddLinkRequestDto;
import com.giftforyoube.funding.entity.FundingItem;
import com.giftforyoube.funding.service.FundingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/funding")
public class FundingController {

    private final FundingService fundingService;

    @PostMapping("/addLink")
    public ResponseEntity<?> addLinkAndSaveToCache(@RequestBody AddLinkRequestDto requestDto) {
        try {
            FundingItem fundingItem = fundingService.previewItem(requestDto.getItemLink());
            fundingService.saveToCache(fundingItem);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error adding link: " + e.getMessage());
        }
    }

    // 캐시에서 아이템 삭제 요청 처리
    @DeleteMapping("/clearCache")
    public ResponseEntity<?> clearCache() {
        try {
            fundingService.clearCache();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error clearing cache: " + e.getMessage());
        }
    }

}
