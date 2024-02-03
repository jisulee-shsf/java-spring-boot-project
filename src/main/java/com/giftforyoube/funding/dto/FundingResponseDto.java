package com.giftforyoube.funding.dto;

import com.giftforyoube.funding.entity.Funding;
import com.giftforyoube.funding.entity.FundingStatus;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter
public class FundingResponseDto implements Serializable {
    private static final long serialVersionUID = 1L; // serialVersionUID 추가

    private Long id;
    private String itemLink;
    private String itemImage;
    private String itemName;
    private String showName;
    private String title;
    private String content;
    private int currentAmount;
    private int targetAmount;
    private boolean publicFlag;
    private LocalDate endDate;
    private String dDay;
    private FundingStatus status;
    private int achievementRate;
    private Long ownerId;
    private boolean ownerFlag;
    private LocalDateTime modifiedAt;

    @Builder
    public FundingResponseDto(Long id, String itemLink, String itemImage, String itemName, String showName, String title, String content, int currentAmount, int targetAmount, boolean publicFlag, LocalDate endDate,String dDay,FundingStatus status, int achievementRate,Long ownerId,boolean ownerFlag,LocalDateTime modifiedAt) {
        this.id = id;
        this.itemLink = itemLink;
        this.itemImage = itemImage;
        this.itemName = itemName;
        this.showName = showName;
        this.title = title;
        this.content = content;
        this.currentAmount = currentAmount;
        this.targetAmount = targetAmount;
        this.publicFlag = publicFlag;
        this.endDate = endDate;
        this.dDay = dDay;
        this.status = status;
        this.achievementRate = achievementRate;
        this.ownerId = ownerId;
        this.ownerFlag = ownerFlag;
        this.modifiedAt = modifiedAt;

        // D-Day 계산
        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), endDate);
        this.dDay = (daysRemaining != 0) ? ((daysRemaining > 0) ? "D-" + daysRemaining : "D+" + Math.abs(daysRemaining)) : "D-Day";

        // 목표금액 달성율 계산
        if (targetAmount == 0) {
            this.achievementRate = 0;
        } else {
            this.achievementRate = (int) Math.round((double) currentAmount / targetAmount * 100);
        }
    }


    public static FundingResponseDto fromEntity(Funding funding) {
        // D-Day와 목표금액 달성율 계산
        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), funding.getEndDate());
        String dDay = (daysRemaining != 0) ? ((daysRemaining > 0) ? "D-" + daysRemaining : "D+" + Math.abs(daysRemaining)) : "D-Day";
        int achievementRate = (funding.getTargetAmount() == 0) ? 0 : (int) Math.round((double) funding.getCurrentAmount() / funding.getTargetAmount() * 100);

        return FundingResponseDto.builder()
                .id(funding.getId())
                .itemLink(funding.getItemLink())
                .itemImage(funding.getItemImage())
                .itemName(funding.getItemName())
                .showName(funding.getShowName())
                .title(funding.getTitle())
                .content(funding.getContent())
                .currentAmount(funding.getCurrentAmount())
                .targetAmount(funding.getTargetAmount())
                .publicFlag(funding.isPublicFlag())
                .endDate(funding.getEndDate())
                .dDay(dDay)
                .status(funding.getStatus())
                .achievementRate(achievementRate)
                .ownerId(funding.getUser().getId())
                .ownerFlag(false)
                .modifiedAt(funding.getModifiedAt())
                .build();
    }

    public void setIsOwner(boolean isOwner) {
        this.ownerFlag = isOwner;
    }
}
