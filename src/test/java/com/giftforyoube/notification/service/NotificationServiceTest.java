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
        receiver = new User("user@example.com", "password", "testNickname", true);
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
        verify(emitterRepository, times(1)).findAllEmitterStartWithByUserId(receiver.getNickname());
    }

    @Test
    @DisplayName("getNotifications 테스트 - NotificationResponseDto List 반환")
    void getNotificationsTest() {
        // Given: User 객체와 Notification 목록 준비
        User user = new User(); // User 객체는 적절히 초기화되어야 함
        List<Notification> notifications = List.of(new Notification()); // Notification 객체들의 목록 준비
        when(notificationRepository.findAllByReceiverOrderByCreatedAtDesc(any(User.class))).thenReturn(notifications);

        // When: getNotifications 메소드 실행
        List<NotificationResponseDto> result = notificationService.getNotifications(user);

        // Then: 반환된 리스트가 비어있지 않은지 확인
        assertFalse(result.isEmpty(), "Result should not be empty");
    }

    @Test
    @DisplayName("readNotifications 테스트 - isRead 상태 변환 확인")
    void whenReadNotification_thenNotificationIsMarkedAsRead() {
        // Given: User 객체와 Notification 객체 준비
        User user = new User(); // User 객체는 적절히 초기화되어야 함
        Notification notification = new Notification(); // Notification 객체는 적절히 초기화되어야 함
        notification.setIsRead(false);
        when(notificationRepository.findById(anyLong())).thenReturn(Optional.of(notification));

        // When: readNotification 메소드 실행
        NotificationResponseDto result = notificationService.readNotification(user, 1L);

        // Then: 반환된 NotificationResponseDto가 읽음 처리된 상태인지 확인
        assertTrue(result.getIsRead(), "Notification should be marked as read");
    }

    @Test
    @DisplayName("deleteNotification 테스트 - 삭제 확인")
    void whenDeleteNotification_thenNotificationIsDeleted() {
        // Given: User 객체와 Notification 객체 준비
        User user = new User(); // User 객체는 적절히 초기화되어야 함
        Notification notification = new Notification(); // Notification 객체는 적절히 초기화되어야 함
        when(notificationRepository.findById(anyLong())).thenReturn(Optional.of(notification));

        // When: deleteNotification 메소드 실행
        notificationService.deleteNotification(user, 1L);

        // Then: notificationRepository의 delete 메소드가 호출되었는지 확인
        verify(notificationRepository, times(1)).delete(any(Notification.class));
    }
}
