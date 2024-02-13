package com.giftforyoube.donation.entity;

import com.giftforyoube.funding.entity.Funding;
import com.giftforyoube.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String sponsorNickname;

    @Column
    private String sponsorComment;

    @Column
    private int donationAmount;

    @Column
    private int donationRanking;

    @Column
    private String fundingShowName; // TEST

    @Column
    private String userNickname; // TEST

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funding_id")
    private Funding funding;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public Donation(String sponsorNickname, String sponsorComment, int donationAmount, int donationRanking, Funding funding, User user) {
        this.sponsorNickname = sponsorNickname;
        this.sponsorComment = sponsorComment;
        this.donationAmount = donationAmount;
        this.donationRanking = donationRanking;
        this.funding = funding;
        this.fundingShowName = funding.getShowName();
        if (user != null) {
            this.user = user;
            this.userNickname = user.getNickname();
        } else {
            this.userNickname = null;
        }
    }
}