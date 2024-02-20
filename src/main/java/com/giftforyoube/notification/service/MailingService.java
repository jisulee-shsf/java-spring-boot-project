package com.giftforyoube.notification.service;

import com.giftforyoube.notification.entity.Notification;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Random;

@Component
@RequiredArgsConstructor
public class MailingService {

    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine;
    private static final String senderEmail = "wamandoo95@gmail.com";
    private static int authenticationCode;
    private static final String EMAIL_TITLE_PREFIX = "[Giftipie] ";

    @Async
    public void sendNotificationEmail(Notification notification) throws MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper messageHelper = new MimeMessageHelper(message, true, "UTF-8");

        // 메일 제목
        messageHelper.setSubject(EMAIL_TITLE_PREFIX + notification.getContent());

        // 메일 받는 사람 설정
        messageHelper.setTo(notification.getReceiver().getEmail());

        // 템플릿에 전달할 데이터 설정
        Context context = new Context();

        String html = "";
        // 메일 내용
        switch (notification.getNotificationType()) {
            case DONATION :
                html = templateEngine.process("EmailTemplate", context);
                break;
            case FUNDING_SUCCESS :
                html = templateEngine.process("EmailTemplate", context);
                break;
            case FUNDING_TIME_OUT :
                html = templateEngine.process("EmailTemplate", context);
                break;
        }

        messageHelper.setText(html, true);

        // 이메일에 포함된 이미지 설정
//        messageHelper.addInline("logo", new ClassPathResource("templates/images/image-2.png"));
//        messageHelper.addInline("notice-icon", new ClassPathResource("templates/images/image-1.png"));

        // 메일 전송
        javaMailSender.send(message);
    }

    // 랜덤 숫자 생성
    public static void createCode() {
        Random random = new Random();
        authenticationCode = random.nextInt(9000) + 1000; // 1000에서 9999 사이 랜덤숫자
    }

    // 회원가입 검증 이메일 생성
    public MimeMessage createMail(String mail) {
        createCode();
        MimeMessage message = javaMailSender.createMimeMessage();

        try {
            // 이메일 주소 형식 검증
            InternetAddress emailAddress = new InternetAddress(mail);
            emailAddress.validate();

            message.setFrom(senderEmail);
            message.setRecipients(MimeMessage.RecipientType.TO, String.valueOf(emailAddress));
            message.setSubject(EMAIL_TITLE_PREFIX + "이메일 인증 코드입니다.");
            String body = "";
            body += "<h3>" + "안녕하세요. 기프티파이입니다." + "</h3>";
            body += "<h3>" + "요청하신 인증 번호 입니다." + "</h3>";
            body += "<h1>" + authenticationCode + "</h1>";
            body += "<h3>" + "감사합니다!" + "</h3>";
            message.setText(body, "UTF-8", "html");

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }

        return message;
    }

    // 회원가입 이메일 검증 메일 전송
    public int sendMail(String mail) {
        MimeMessage message = createMail(mail);
        javaMailSender.send(message);

        return authenticationCode;
    }
}
