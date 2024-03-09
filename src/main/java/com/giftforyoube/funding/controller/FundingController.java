package com.giftforyoube.funding.controller;

import com.giftforyoube.funding.dto.*;
import com.giftforyoube.funding.service.FundingService;
import com.giftforyoube.global.exception.BaseException;
import com.giftforyoube.global.exception.BaseResponse;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.global.security.UserDetailsImpl;
import com.giftforyoube.user.entity.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/funding")
@Tag(name = "펀딩", description = "펀딩 관련 API")
public class FundingController {

    private final FundingService fundingService;

    // 링크 추가 및 캐시 저장 요청 처리
    @PostMapping("/addLink")
    public ResponseEntity<BaseResponse<FundingItemResponseDto>> addLinkAndSaveToCache(@RequestBody AddLinkRequestDto requestDto, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        log.info("[addLinkAndSaveToCache] 상품링크: " + requestDto);

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new BaseResponse<>(BaseResponseStatus.UNAUTHORIZED_TO_ADD_LINK));
        }
        try {
            FundingItemResponseDto fundingItemResponseDto = fundingService.addLinkAndSaveToCache(requestDto, userDetails.getUser().getId());
            return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.FUNDING_ITEM_LINK_SUCCESS, fundingItemResponseDto));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new BaseResponse<>(BaseResponseStatus.FUNDING_ITEM_LINK_FAILED));
        }
    }

    // 펀딩 상세 정보 입력 및 DB 저장 요청 처리
    @PostMapping("/create")
    public ResponseEntity<BaseResponse<FundingResponseDto>> createFunding(@RequestBody FundingCreateRequestDto requestDto,@AuthenticationPrincipal UserDetailsImpl userDetails) {
        log.info("[createFunding] 펀딩등록: " + requestDto);

        if(userDetails == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new BaseResponse<>(BaseResponseStatus.UNAUTHORIZED_TO_CREATE_FUNDING));
        }
        Long userId = userDetails.getUser().getId();
        try {
            FundingResponseDto responseDto = fundingService.saveToDatabase(requestDto,userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(new BaseResponse<>(BaseResponseStatus.FUNDING_CREATE_SUCCESS, responseDto));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new BaseResponse<>(BaseResponseStatus.FUNDING_CREATE_FAILED));
        }
    }

    // 내 펀딩 정보를 조회하는 API
    @GetMapping("/myFunding")
    public ResponseEntity<?> getMyFunding(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            // 로그인하지 않은 사용자가 API를 호출하면 적절한 HTTP 상태 코드와 메시지를 반환
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new BaseResponse<>(BaseResponseStatus.UNAUTHORIZED_TO_GET_MY_FUNDING));
        }
        FundingResponseDto fundingResponseDto = fundingService.getMyFundingInfo(userDetails.getUser());
        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.MY_FUNDING_GET_SUCCESS, fundingResponseDto));
    }

    @GetMapping("")
    public ResponseEntity<BaseResponse<Page<FundingResponseDto>>> getActiveMainFunding(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder
    ){
        log.info("[getActiveFunding] 메인페이지 진행중인 펀딩 조회");

        Page<FundingResponseDto> activeFundingsPage = fundingService.getActiveMainFunding(page, size, sortBy, sortOrder);
        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.ACTIVE_MAIN_FUNDING_GET_SUCCESS, activeFundingsPage));
    }

    // Slice - Page 페이지네이션 수정 적용
    @GetMapping("/all")
    public ResponseEntity<BaseResponse<Page<FundingResponseDto>>> getAllFundings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder
    ){
        log.info("[getAllFundings] 모든 펀딩 리스트 조회 무한스크롤");

        Page<FundingResponseDto> allFundingsPage = fundingService.getAllFundings(page, size, sortBy, sortOrder);
        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.ALL_FUNDING_GET_SUCCESS, allFundingsPage));
    }

    @GetMapping("/active")
    public ResponseEntity<BaseResponse<Slice<FundingResponseDto>>> getActiveFundings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder
    ){
        log.info("[getActiveFundings] 진행중인 펀딩 리스트 조회 무한스크롤");

        Slice<FundingResponseDto> activeFundingsPage = fundingService.getActiveFundings(page, size, sortBy, sortOrder);
        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.ACTIVE_FUNDINGS_GET_SUCCESS, activeFundingsPage));
    }

    // 펀딩 등록시 저장된 마감일 기준으로 현재 종료된 펀딩 [페이지네이션 적용]
    @GetMapping("/finished")
    public ResponseEntity<BaseResponse<Slice<FundingResponseDto>>> getFinishedFundings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder
    ){
        log.info("[getFinishedFundings] 완료된 펀딩 리스트 조회 무한스크롤");
        Slice<FundingResponseDto> finishedFundingsPage = fundingService.getFinishedFundings(page, size, sortBy, sortBy);
        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.FINISHED_FUNDINGS_GET_SUCCESS, finishedFundingsPage));
    }

    // D-Day를 포함한 펀딩 상세 페이지
    @GetMapping("/{fundingId}")
    public ResponseEntity<BaseResponse<FundingResponseDto>> findFunding(@PathVariable Long fundingId, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        log.info("[findFunding] 펀딩 상세 페이지" + fundingId);

        User user = null;
        if (userDetails != null) {
            user = userDetails.getUser();
        }
        // 캐시된 데이터를 사용하여 FundingResponseDto를 얻습니다.
        FundingResponseDto fundingResponseDto = fundingService.findFunding(fundingId);
        log.info("[findFunding] 펀딩 상세 페이지" + fundingResponseDto);

        // 여기에서는 isOwner 값을 동적으로 설정합니다.
        boolean isOwner = user != null && fundingResponseDto.getOwnerId().equals(user.getId());
        fundingResponseDto.setIsOwner(isOwner);

        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.FUNDING_DETAIL_GET_SUCCESS, fundingResponseDto));
    }

    // 펀딩 종료버튼 딸~깍
    @PatchMapping("/{fundingId}/finish")
    public ResponseEntity<?> finishFunding(@PathVariable Long fundingId, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        log.info("[finishFunding] 펀딩 종료하기" + fundingId);

        if(userDetails == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new BaseResponse<>(BaseResponseStatus.UNAUTHORIZED_TO_FINISH_FUNDING));
        }
        fundingService.finishFunding(fundingId, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.OK).body(new BaseResponse<>(BaseResponseStatus.FUNDING_FINISH_SUCCESS));
    }

    // 펀딩 수정
    @PatchMapping("/{fundingId}/update")
    public ResponseEntity<BaseResponse<FundingResponseDto>> updateFunding(@PathVariable Long fundingId,
                                                            @AuthenticationPrincipal UserDetailsImpl userDetails,
                                                            @RequestBody FundingUpdateRequestDto requestDto) {
        if(userDetails == null){
            throw new BaseException(BaseResponseStatus.AUTHENTICATION_FAILED);
        }
        fundingService.updateFunding(fundingId, userDetails.getUser(), requestDto);
        return ResponseEntity.status(HttpStatus.OK).body(new BaseResponse<>(BaseResponseStatus.FUNDING_UPDATE_SUCCESS));
    }

    // 펀딩 삭제
    @DeleteMapping("/{fundingId}")
    public ResponseEntity<BaseResponse<FundingResponseDto>> deleteFunding(@PathVariable Long fundingId, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            throw new BaseException(BaseResponseStatus.AUTHENTICATION_FAILED);
        }
        fundingService.deleteFunding(fundingId, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.OK).body(new BaseResponse<>(BaseResponseStatus.FUNDING_DELETE_SUCCESS));
    }

    @GetMapping("/summary")
    public ResponseEntity<BaseResponse<FundingSummaryResponseDto>> getFundingSummary() {
        FundingSummaryResponseDto summaryResponseDto = fundingService.getFundingSummary();
        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.FUNDINGS_SUMMARY_GET_SUCCESS, summaryResponseDto));
    }
}