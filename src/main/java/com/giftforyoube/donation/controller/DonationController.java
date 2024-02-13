package com.giftforyoube.donation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DonationController {

    @GetMapping("/donation")
    public String showDonation() {
        return "donation";
    }
}