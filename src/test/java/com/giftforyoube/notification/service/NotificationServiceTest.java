package com.giftforyoube.notification.service;

import com.giftforyoube.notification.dto.NotificationResponseDto;
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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    private User receiver;
    private Notification notification;
    private NotificationType notificationType;
    private String content;
    private String url;

    @BeforeEach
    void setUp() {
        // 테스트를 위한 데이터 세팅
        receiver = User.builder()
                .email("user@example.com")
                .password("password")
                .nickname("testNickname")
                .isEmailNotificationAgreed(true)
                .build();

        receiver.setId(1L);
        notificationType = NotificationType.DONATION;
        content = "Test content";
        url = "test.com";

        notification = Notification.builder()
                .receiver(receiver)
                .notificationType(notificationType)
                .content(content)
                .url(url)
                .isRead(false)
                .build();
    }

    @Test
    @DisplayName("sseSubscribe 테스트 - sseEmitter 반환")
    void sseSubscribeTest() {
        // given : Mock HttpServletResponse 준비
        MockHttpServletResponse response = new MockHttpServletResponse();
        String username = "testUser";
        String lastEventId = "0";
        when(emitterRepository.save(anyString(), any(SseEmitter.class))).thenAnswer(i -> i.getArguments()[1]);

        // when : sseSubscribe 메서드 실행
        SseEmitter emitter = notificationService.sseSubscribe(username, lastEventId, response);

        // Then: 반환된 SseEmitter가 null이 아닌지 확인
        assertNotNull(emitter, "SseEmitter null 아님 체크완료");
        // Then: 헤더 설정이 적절히 이루어졌는지 검증
        assertEquals("no", response.getHeader("X-Accel-Buffering"), "Header X-Accel-Buffering 헤더 'no' 체크완료");
    }

    @Test
    @DisplayName("send 테스트 - 알림발생, 전송, 메일발송")
    void sendTest() throws MessagingException {
        // given : 알림 객체를 DB에 저장하고, 이메일 전송을 임의로 설정
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(emitterRepository.findAllEmitterStartWithByUserId(anyString())).thenReturn(Collections.emptyMap());
        doNothing().when(mailingService).sendNotificationEmail(any(Notification.class));

        // when : send 메서드 실행
        notificationService.send(receiver, notificationType, content, url);

        // then : 알림 객체가 저장되었는지 확인
        verify(notificationRepository, times(1)).save(any(Notification.class));
        // then : 이메일이 전송되었는지 확인
        verify(mailingService, times(1)).sendNotificationEmail(any(Notification.class));
        // then : SSE Emitter 저장소가 조회되었는지 확인
        verify(emitterRepository, times(1)).findAllEmitterStartWithByUserId(receiver.getEmail());
    }

    @Test
    @DisplayName("getNotifications 테스트 - NotificationResponseDto List 반환")
    void getNotificationsTest() {
        // given: 사용자에 대한 알림 목록을 스터빙으로 설정
        List<Notification> notifications = List.of(
                Notification.builder()
                        .receiver(receiver)
                        .notificationType(NotificationType.DONATION)
                        .content("Donation received")
                        .url("donation.com")
                        .isRead(false)
                        .build(),
                Notification.builder()
                        .receiver(receiver)
                        .notificationType(NotificationType.FUNDING_TIME_OUT)
                        .content("Thank you for your donation")
                        .url("thankyou.com")
                        .isRead(true)
                        .build()
        );
        when(notificationRepository.findAllByReceiverOrderByCreatedAtDesc(receiver)).thenReturn(notifications);

        // when: getNotifications 메서드 실행
        List<NotificationResponseDto> result = notificationService.getNotifications(receiver);

        // then: 반환된 알림 목록의 크기가 예상과 일치하는지 확인
        assertEquals(2, result.size(), "Returned notifications count should match expected");

        // then: 반환된 알림의 내용이 예상과 일치하는지 확인
        NotificationResponseDto firstNotification = result.get(0);
        assertEquals("Donation received", firstNotification.getContent(), "First notification content should match");
        assertEquals("donation.com", firstNotification.getUrl(), "First notification URL should match");
        assertFalse(firstNotification.getIsRead(), "First notification read status should be false");

        NotificationResponseDto secondNotification = result.get(1);
        assertEquals("Thank you for your donation", secondNotification.getContent(), "Second notification content should match");
        assertEquals("thankyou.com", secondNotification.getUrl(), "Second notification URL should match");
        assertTrue(secondNotification.getIsRead(), "Second notification read status should be true");
    }

    @Test
    @DisplayName("readNotifications 테스트 - 특정 알림 읽음 처리")
    void readNotificationTest() {
        // given: 읽으려는 알림의 ID 설정
        Long notificationId = 1L;

        // Notification 객체 준비
        notification.setId(notificationId); // 알림 ID 설정

        // NotificationRepository.findById 스터빙: 특정 ID의 Notification 객체를 반환하도록 설정
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        // NotificationRepository.save 스터빙: 반환되는 Notification 객체가 읽음 상태로 업데이트 되었는지 확인
        when(notificationRepository.save(any(Notification.class))).then(returnsFirstArg());

        // when: readNotification 메서드 실행
        NotificationResponseDto responseDto = notificationService.readNotification(receiver, notificationId);

        // then: 반환된 NotificationResponseDto 객체가 null이 아닌지 확인
        assertNotNull(responseDto, "NotificationResponseDto should not be null");

        // then: 알림의 읽음 상태가 true로 업데이트 되었는지 확인
        assertTrue(responseDto.getIsRead(), "Notification should be marked as read");

        // NotificationRepository.save가 호출되었는지 확인
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("deleteNotification 테스트 - 특정 알림 삭제 처리")
    void deleteNotificationTest() {
        // given: 삭제하려는 알림의 ID 설정
        Long notificationId = 1L;

        // Notification 객체 준비 및 NotificationRepository.findById 스터빙
        // setUp 메서드에서 이미 초기화된 notification 객체를 사용
        notification.setId(notificationId); // 알림 ID 설정
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        // when: deleteNotification 메서드 실행
        notificationService.deleteNotification(receiver, notificationId);

        // then: NotificationRepository.delete가 호출되었는지 확인
        verify(notificationRepository, times(1)).delete(notification);
    }
}
