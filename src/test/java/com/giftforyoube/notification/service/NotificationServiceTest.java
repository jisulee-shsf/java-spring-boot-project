package com.giftforyoube.notification.service;


import com.giftforyoube.notification.entity.Notification;
import com.giftforyoube.notification.entity.NotificationType;
import com.giftforyoube.notification.repository.EmitterRepository;
import com.giftforyoube.notification.repository.NotificationRepository;
import com.giftforyoube.user.entity.User;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock
    private EmitterRepository emitterRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private MailingService mailingService;

    @InjectMocks
    private NotificationService notificationService;

    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60;

    @BeforeEach
    void setUp() {
//        service = new NotificationService(mockEmitterRepository, mockNotificationRepository, mockMailingService);
    }

    @Test
    @DisplayName("메시지 전송 테스트")
    void givenNotificationDetails_whenSend_thenNotificationIsSavedAndSent() {
        // given
        String nickname = "userNickname"; // `User` 객체에 닉네임 설정
        User receiver = new User(nickname); // 사용자 객체를 적절히 생성 또는 모킹
        NotificationType notificationType = NotificationType.DONATION;
        String content = "Test Notification";
        String url = "http://example.com";
        Notification mockNotification = new Notification(); // 적절한 생성자 또는 빌더 사용
        when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);
        when(emitterRepository.findAllEmitterStartWithByUserId(anyString())).thenReturn(Collections.emptyMap());

        // when
        notificationService.send(receiver, notificationType, content, url);

        // then
        verify(notificationRepository).save(any(Notification.class));
        try {
            verify(mailingService).sendNotificationEmail(any(Notification.class));
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        verify(emitterRepository, atLeastOnce()).findAllEmitterStartWithByUserId(anyString());
    }


    @Test
    @DisplayName("알림 메시지 포맷 및 내용 검증 테스트")
    void givenNotification_whenSendNotification_thenCorrectFormatAndContent() {
        // given
        String nickname = "userNickname"; // `User` 객체에 닉네임 설정
        User receiver = new User(nickname); // 사용자 객체를 적절히 생성 또는 모킹
        NotificationType notificationType = NotificationType.DONATION;
        String content = "Notification Content";
        String url = "http://example.com";
        Notification mockNotification = Notification.builder()
                .receiver(receiver)
                .notificationType(notificationType)
                .content(content)
                .url(url)
                .isRead(false)
                .build();
        when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);
        when(emitterRepository.findAllEmitterStartWithByUserId(anyString())).thenReturn(Collections.emptyMap());

        // when
        notificationService.send(receiver, notificationType, content, url);

        // then
        verify(notificationRepository).save(argThat(notification ->
                notification.getContent().equals(content) &&
                        notification.getUrl().equals(url) &&
                        notification.getNotificationType() == notificationType));
    }
}
