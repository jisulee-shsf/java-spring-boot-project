package com.giftforyoube.funding.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class FundingSummary {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false)
        private long totalDonationsCount = 0;

        @Column(nullable = false)
        private long successfulFundingsCount = 0;

        @Column(nullable = false)
        private long totalFundingAmount = 0;
}
