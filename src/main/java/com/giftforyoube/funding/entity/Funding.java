package com.giftforyoube.funding.entity;

import com.giftforyoube.donation.entity.Donation;
import com.giftforyoube.funding.dto.FundingCreateRequestDto;
import com.giftforyoube.global.entity.Auditable;
import com.giftforyoube.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class Funding extends Auditable implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 1000)
    private String itemLink;
    private String itemImage;
    private String itemName;
    private String showName;
    private String title;
    @Column(length = 1000)
    private String content;
    private int currentAmount;
    private int targetAmount;
    private boolean publicFlag;
    private LocalDate endDate;
    @Enumerated(EnumType.STRING)
    private FundingStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "funding", fetch = FetchType.LAZY)
    private List<Donation> donations = new ArrayList<>();

    @Builder
    public Funding(String itemLink, String itemImage, String itemName,String showName,String title, String content, int currentAmount, int targetAmount, boolean publicFlag, LocalDate endDate,FundingStatus status) {
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
        this.status = status;
    }

    public void update(FundingCreateRequestDto requestDto) {
        this.itemName = requestDto.getItemName();
        this.showName = requestDto.getShowName();
        this.title = requestDto.getTitle();
        this.content = requestDto.getContent();
        this.targetAmount = requestDto.getTargetAmount();
        this.publicFlag = requestDto.isPublicFlag();
        this.endDate = requestDto.getEndDate();
    }
}