package com.giftforyoube.notification.controller;

import com.giftforyoube.notification.dto.MailRequestDto;
import com.giftforyoube.notification.service.MailingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "이메일", description = "이메일 관련 API")
public class MailController {
    private final MailingService mailService;

    // 인증 이메일 전송
    @PostMapping("/mailSend")
    public HashMap<String, Object> mailSend(@RequestBody MailRequestDto mail) {
        HashMap<String, Object> map = new HashMap<>();

        try {
            String codeStr = String.valueOf(mailService.sendMail(mail.getMail()));

            map.put("success", Boolean.TRUE);
            map.put("code", codeStr);
        } catch (Exception e) {
            map.put("success", Boolean.FALSE);
            map.put("error", e.getMessage());
        }

        return map;
    }
}
