package com.giftforyoube.funding.controller;

import com.giftforyoube.funding.dto.AddLinkRequestDto;
import com.giftforyoube.funding.dto.FundingCreateRequestDto;
import com.giftforyoube.funding.dto.FundingResponseDto;
import com.giftforyoube.funding.entity.FundingItem;
import com.giftforyoube.funding.service.FundingService;
import com.giftforyoube.global.security.UserDetailsImpl;
import com.giftforyoube.user.entity.User;
import com.giftforyoube.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/funding")
public class FundingController {

    private final FundingService fundingService;
    private final UserService userService;

    // 링크 추가 및 캐시 저장 요청 처리
    @PostMapping("/addLink")
    public ResponseEntity<?> addLinkAndSaveToCache(@RequestBody AddLinkRequestDto requestDto, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            throw new NullPointerException("링크 등록을 하려면 로그인을 해야합니다.");
        }
        try {
            fundingService.addLinkAndSaveToCache(requestDto, userDetails.getUser().getId());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error adding link: " + e.getMessage());
        }
    }

    // 펀딩 상세 정보 입력 및 DB 저장 요청 처리
    @PostMapping("/create")
    public ResponseEntity<?> createFunding(@RequestBody FundingCreateRequestDto requestDto,@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if(userDetails == null){
            throw new NullPointerException("펀딩 등록을 하려면 로그인을 해야합니다.");
        }
        Long userId = userDetails.getUser().getId();
        try {
            FundingResponseDto responseDto = fundingService.saveToDatabase(requestDto,userId);
            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating funding: " + e.getMessage());
        }
    }

    // 펀딩 등록시 저장된 마감일 기준으로 현재 진행중인 펀딩
    @GetMapping("/active")
    public ResponseEntity<List<FundingResponseDto>> getActiveFundings(){
        List<FundingResponseDto> activeFundings = fundingService.getActiveFundings();
        return ResponseEntity.ok(activeFundings);
    }

    // 펀딩 등록시 저장된 마감일 기준으로 현재 종료된 펀딩
    @GetMapping("/finished")
    public ResponseEntity<List<FundingResponseDto>> getFinishedFundings(){
        List<FundingResponseDto> finishedFundings = fundingService.getFinishedFunding();
        return ResponseEntity.ok(finishedFundings);
    }

    // D-Day를 포함한 펀딩 상세 페이지
    @GetMapping("/{fundingId}")
    public ResponseEntity<FundingResponseDto> findFunding(@PathVariable Long fundingId, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = null;
        if (userDetails != null) {
            user = userDetails.getUser();
        }
        // 캐시된 데이터를 사용하여 FundingResponseDto를 얻습니다.
        FundingResponseDto fundingResponseDto = fundingService.findFunding(fundingId);
        // 여기에서는 isOwner 값을 동적으로 설정합니다.
        boolean isOwner = user != null && fundingResponseDto.getOwnerId().equals(user.getId());
        fundingResponseDto.setIsOwner(isOwner);

        return ResponseEntity.ok(fundingResponseDto);
    }

    @PatchMapping("/{fundingId}/finish")
    public ResponseEntity<?> finishFunding(@PathVariable Long fundingId, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if(userDetails == null){
            throw new NullPointerException("로그인을 해주십쇼");
        }
        try {
            fundingService.finishFunding(fundingId, userDetails.getUser());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error finishing funding: " + e.getMessage());
        }
    }
}