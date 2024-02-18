package com.giftforyoube.user.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/login")
public class UserRedirectController {

    @GetMapping("/success")
    public String redirectToLoginSuccessUri() {
        return "redirect:/";
    }

    @GetMapping("/fail")
    public String redirectToLoginFailUri() {
        return "redirect:/";
    }
}