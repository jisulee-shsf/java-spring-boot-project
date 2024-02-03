package com.giftforyoube.notification.service;

import com.giftforyoube.notification.entity.Notification;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Component
@RequiredArgsConstructor
public class MailingService {

    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine;

    private static final String EMAIL_TITLE_PREFIX = "[Giftipie] ";

    @Async
    public void sendNotificationEmail(Notification notification) throws MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper messageHelper = new MimeMessageHelper(message, true, "UTF-8");

        // 메일 제목
        messageHelper.setSubject(EMAIL_TITLE_PREFIX + notification.getContent());

        // 메일 받는 사람 설정
//        messageHelper.setTo(notification.getReceiver().getEmail());
        messageHelper.setTo("kingmandoo95@gmail.com"); // 개인 개정으로 테스트용

        // 템플릿에 전달할 데이터 설정
        Context context = new Context();

        // 메일 내용
        String html = templateEngine.process("EmailTemplate", context);
        messageHelper.setText(html, true);

        // 이메일에 포함된 이미지 설정
//        messageHelper.addInline("logo", new ClassPathResource("templates/images/image-2.png"));
//        messageHelper.addInline("notice-icon", new ClassPathResource("templates/images/image-1.png"));

        // 메일 전송
        javaMailSender.send(message);
    }
}
