package com.giftforyoube.notification.service;

import com.giftforyoube.global.exception.BaseException;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.notification.entity.Notification;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Random;

@Component
@RequiredArgsConstructor
public class MailingService {

    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine;
    private static final String EMAIL_TITLE_PREFIX = "[Giftipie] ";

    @Value("${spring.mail.username}")
    private String senderEmail;

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
                html = templateEngine.process("EmailTemplateDonation", context);
                break;
            case FUNDING_SUCCESS :
                html = templateEngine.process("EmailTemplateFundingSuccess", context);
                break;
            case FUNDING_TIME_OUT :
                html = templateEngine.process("EmailTemplateTimeOut", context);
                break;
        }

        messageHelper.setText(html, true);

        // 이메일에 포함된 이미지 설정
//        messageHelper.addInline("logo", new ClassPathResource("templates/images/image-2.png"));
//        messageHelper.addInline("notice-icon", new ClassPathResource("templates/images/image-1.png"));

        // 메일 전송
        javaMailSender.send(message);
    }

    ////////////////////////////////////////////////////////////////

    // 회원가입 검증 이메일 생성
    public MimeMessage createSignupMail(String mail, int authenticationCode) throws MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        // 이메일 주소 형식 검증
        InternetAddress emailAddress = new InternetAddress(mail);
        emailAddress.validate();

        // 메일 세팅
        // 이메일 제목
        helper.setSubject(EMAIL_TITLE_PREFIX + "이메일 인증 코드입니다.");

        // 이메일 보내는 사람
        helper.setFrom(senderEmail);

        // 이메일 받는 사람
        helper.setTo(mail);

        // 템플릿에 전달할 데이터
        Context context = new Context();
        context.setVariable("authenticationCode", authenticationCode); // 추가된부분

//        String body = "";
//        body += "<h3>" + "안녕하세요. 기프티파이입니다." + "</h3>";
//        body += "<h3>" + "요청하신 인증 번호 입니다." + "</h3>";
//        body += "<h1>" + authenticationCode + "</h1>";
//        body += "<h3>" + "감사합니다!" + "</h3>";
        String htmlContent = templateEngine.process("EmailTemplateSignup", context);

        helper.setText(htmlContent, true);

        return message;
    }

    // 회원가입 이메일 검증 메일 전송한 후, 인증코드 반환
    public int sendMail(String mail) {
        Random random = new Random();
        int authenticationCode = random.nextInt(9000) + 1000; // 1000에서 9999 사이 랜덤 숫자 생성

        try {
            // 메일 생성 및 전송
            MimeMessage message = createSignupMail(mail, authenticationCode);
            javaMailSender.send(message);
        } catch (MessagingException e) {
            throw new BaseException(BaseResponseStatus.EMAIL_SEND_FAILED);
        }

        return authenticationCode;
    }
}
