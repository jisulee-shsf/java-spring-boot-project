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
    private String comment;

    @Column
    private int donationAmount;

    @Column
    private String fundingItemName; // TEST

    @Column
    private String userNickname; // TEST

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funding_id")
    private Funding funding;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public Donation(String sponsorNickname, String comment, int donationAmount, Funding funding, User user) {
        this.sponsorNickname = sponsorNickname;
        this.comment = comment;
        this.donationAmount = donationAmount;
        this.funding = funding;
        this.fundingItemName = funding.getItemName();
        if (user != null) {
            this.user = user;
            this.userNickname = user.getNickname();
        } else {
            this.userNickname = null;
        }
    }
}