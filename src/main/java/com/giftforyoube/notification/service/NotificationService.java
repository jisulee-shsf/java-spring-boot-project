package com.giftforyoube.notification.service;

import com.giftforyoube.global.exception.BaseException;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.notification.dto.MessageResponseDto;
import com.giftforyoube.notification.dto.NotificationResponseDto;
import com.giftforyoube.notification.dto.SubscribeDummyDto;
import com.giftforyoube.notification.entity.Notification;
import com.giftforyoube.notification.entity.NotificationType;
import com.giftforyoube.notification.repository.EmitterRepository;
import com.giftforyoube.notification.repository.NotificationRepository;
import com.giftforyoube.user.entity.User;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60; // SSE 연결 지속 시간 (1시간)
    private final EmitterRepository emitterRepository;
    private final NotificationRepository notificationRepository;
    private final MailingService mailingService;

    /**
     * 사용자가 SSE(Server-Sent Events)를 통해 실시간 알림을 구독할 수 있도록 합니다.
     * 이 메서드는 새 SseEmitter 객체를 생성하고, Nginx 버퍼링 문제를 회피하기 위한 헤더 설정,
     * SSE 연결의 완료, 시간 초과, 에러 처리를 구성합니다.
     * 또한, 클라이언트가 미수신한 이벤트가 있을 경우, 이를 전송하여 이벤트 유실을 방지합니다.
     *
     * @param username 로그인 중인 사용자의 이름
     * @param lastEventId 클라이언트가 마지막으로 수신한 이벤트의 ID. 이는 미수신 이벤트 전송 로직에서 사용됩니다.
     * @param response HttpServletResponse 객체, SSE 설정에 필요한 HTTP 헤더 설정에 사용
     * @return SseEmitter 객체, 클라이언트에게 실시간 알림을 전송하기 위한 객체
     */
    public SseEmitter sseSubscribe(String username, String lastEventId, HttpServletResponse response) {
        log.info("sse 연결 시작...");

        // 사용자별 고유 Emitter ID 생성. 현재 시간을 포함하여 중복 방지
        String emitterId = createTimeIncludeId(username);

        // SseEmitter 객체 생성 및 저장. 기본 타임아웃을 사용하여 자동 연결 종료 관리
        SseEmitter emitter = emitterRepository.save(emitterId, new SseEmitter(DEFAULT_TIMEOUT));

        // Nginx를 사용하는 환경에서 SSE 버퍼링 문제 해결을 위한 헤더 설정
        response.setHeader("X-Accel-Buffering", "no");

        // SSE 연결 종료(완료, 시간 초과, 에러) 시 Emitter 저장소에서 해당 Emitter 삭제
        emitter.onCompletion(() -> emitterRepository.deleteAllEmitterStartWithId(emitterId));
        emitter.onTimeout(() -> emitterRepository.deleteAllEmitterStartWithId(emitterId));
        emitter.onError((e) -> emitterRepository.deleteAllEmitterStartWithId(emitterId));

        // 신규 이벤트 ID 생성하여 구독 초기화 이벤트 발송
        String eventId = createTimeIncludeId(username);
        sendNotification(emitter, eventId, emitterId, new SubscribeDummyDto(username));

        // 클라이언트가 이전에 놓친 이벤트가 있는 경우, 해당 이벤트 재전송. Event 유실을 예방
        if (hasLostData(lastEventId)) {
            sendLostData(lastEventId, username, emitterId, emitter);
        }
        log.info("sse 연결 완료");
        return emitter;
    }


    /**
     * 지정된 사용자에게 알림을 전송합니다. 이 메서드는 먼저 알림을 데이터베이스에 저장하고,
     * 해당 사용자의 모든 SSE Emitter에 알림을 전송합니다.
     * 사용자가 이메일 알림 수신에 동의한 경우, 이메일로도 알림을 발송합니다.
     *
     * @param receiver 알림을 받을 사용자 객체
     * @param notificationType 알림의 유형 (펀딩 성공, 펀딩 시간 마감, 후원 발생)
     * @param content 알림에 포함될 메시지 내용
     * @param url 알림과 관련된 자원의 URL
     * @throws BaseException 이메일 발송 실패 시 예외 발생
     */
    public void send(User receiver, NotificationType notificationType, String content, String url) {
        log.info("메세지 전송 시작...");

        // 알림 객체 생성 및 DB에 저장
        Notification notification = createNotification(receiver, notificationType, content, url);
        Notification saveNotification = notificationRepository.save(notification);

        // 알림을 받을 사용자의 ID를 기반으로 고유 이벤트 ID 생성
        String receiverId = receiver.getEmail();
        String eventId = receiverId + "_" + System.currentTimeMillis();

        // 해당 사용자의 모든 SSE Emitter 검색
        Map<String, SseEmitter> emitters = emitterRepository.findAllEmitterStartWithByUserId(receiverId);

        // 각 Emitter에 알림 전송 및 이벤트 캐시에 저장하여 연결 중단 시 재전송 가능하게 함
        emitters.forEach(
                (emitterId, emitter) -> {
                    emitterRepository.saveEventCache(emitterId, saveNotification);
                    sendNotification(emitter, eventId, emitterId, new MessageResponseDto(saveNotification.getId(),
                            saveNotification.getContent(), saveNotification.getUrl(),
                            saveNotification.getNotificationType(), saveNotification.getIsRead(), saveNotification.getCreatedAt()));
                }
        );

        // 사용자가 이메일 알림 수신에 동의한 경우, 이에일로 알림 발송
        if (saveNotification.getReceiver().getIsEmailNotificationAgreed()) {
            log.info("SSE 메시지 전송 완료. 알림 이메일 발송 시작");
            try {
                mailingService.sendNotificationEmail(saveNotification);
            } catch (MessagingException e) {
                throw new BaseException(BaseResponseStatus.EMAIL_SEND_FAILED);
            }
        }
    }

    /**
     * 사용자에게 보낼 알림 객체를 생성합니다.
     *
     * @param receiver 알림을 받을 사용자 객체
     * @param notificationType 알림의 유형
     * @param content 알림에 포함될 내용
     * @param url 알림과 관련된 URL
     * @return 생성된 Notification 객체
     */
    private Notification createNotification(User receiver, NotificationType notificationType, String content, String url) {
        return Notification.builder()
                .receiver(receiver)
                .notificationType(notificationType)
                .content(content)
                .url(url)
                .isRead(false)
                .build();
    }

    /**
     * 사용자에게 누락된 알림 데이터를 전송합니다.
     * 마지막으로 수신한 이벤트 ID 이후의 모든 이벤트를 조회하여 전송합니다.
     *
     * @param lastEventId 사용자가 마지막으로 수신한 이벤트의 ID
     * @param username 사용자의 이름
     * @param emitterId 이벤트를 전송할 SseEmitter의 ID
     * @param emitter 이벤트를 전송할 SseEmitter 객체
     */
    private void sendLostData(String lastEventId, String username, String emitterId, SseEmitter emitter) {
        Map<String, Object> eventCaches = emitterRepository.findAllEventCacheStartWithByUserId(username); // 이벤트 캐시 조회
        eventCaches.entrySet().stream()
                .filter(entry -> lastEventId.compareTo(entry.getKey()) < 0) // 놓친 이벤트 필터링
                .forEach(entry -> sendNotification(emitter, entry.getKey(), emitterId, entry.getValue())); // 놓친 이벤트 전송
    }

    /**
     * 주어진 lastEventId가 비어있지 않은지 확인하여, 누락된 데이터가 있는지 여부를 반환합니다.
     *
     * @param lastEventId 사용자가 마지막으로 수신한 이벤트의 ID
     * @return 누락된 데이터가 있으면 true, 그렇지 않으면 false
     */
    private boolean hasLostData(String lastEventId) {
        return !lastEventId.isEmpty();
    }

    /**
     * 주어진 SseEmitter를 사용하여 알림을 전송합니다.
     * 전송 중 오류가 발생하면, 해당 emitter를 삭제하고 예외를 발생시킵니다.
     *
     * @param emitter 알림을 전송할 SseEmitter 객체
     * @param eventId 알림 이벤트의 ID
     * @param emitterId 알림을 전송할 SseEmitter의 ID
     * @param data 전송할 데이터
     * @throws BaseException 알림 전송 실패 시
     */
    private void sendNotification(SseEmitter emitter, String eventId, String emitterId, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .id(eventId)
                    .name("sse")
                    .data(data)
            );
        } catch (IOException exception) {
            emitterRepository.deleteById(emitterId);
            throw new BaseException(BaseResponseStatus.NOTIFICATION_SEND_FAILED);
        }
    }

    /**
     * 사용자명과 현재 시간을 결합하여 고유한 ID를 생성합니다.
     * 이 ID는 알림이나 이벤트의 식별자로 사용될 수 있습니다.
     *
     * @param username 사용자명
     * @return 생성된 고유 ID 문자열
     */
    private String createTimeIncludeId(String username) {
        return username + "_" + System.currentTimeMillis();
    }

    // user로 해당 user의 전체 알림목록을 조회
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getNotifications(User user) {
        List<Notification> notificationList = notificationRepository.findAllByReceiverOrderByCreatedAtDesc(user);
        return notificationList.stream().map(NotificationResponseDto::new).toList();
    }

    /**
     * 지정된 알림을 읽음으로 표시합니다. 사용자가 해당 알림의 수신자인 경우에만 읽음 처리가 가능합니다.
     *
     * @param user 알림을 읽으려는 사용자 객체
     * @param notificationId 읽음 처리할 알림의 ID
     * @return 읽음 처리된 알림에 대한 응답 DTO
     * @throws BaseException 알림이 존재하지 않거나, 사용자가 알림의 수신자가 아닐 경우 예외 발생
     */
    @Transactional
    public NotificationResponseDto readNotification(User user, Long notificationId) {
        // 알림 ID로 알림 객체 조회, 없으면 예외 발생
        Notification notification = notificationRepository.findById(notificationId).orElseThrow(
                () -> new BaseException(BaseResponseStatus.NOTIFICATION_NOT_FOUND));

        // 요청한 사용자가 알림의 수신자가 아니면 권한 예외 발생
        if (!notification.getReceiver().getId().equals(user.getId())) {
            throw new BaseException(BaseResponseStatus.UNAUTHORIZED_READ_NOTIFICATION);
        }

        // 알림을 읽음으로 표시 후 저장
        notification.setIsRead(true);
        Notification saveNotification = notificationRepository.save(notification);

        return new NotificationResponseDto(saveNotification);
    }

    /**
     * 사용자가 읽은 모든 알림 메시지를 삭제합니다. 사용자에게 읽은 알림이 없을 경우 예외를 발생시킵니다.
     *
     * @param user 알림을 삭제할 사용자 객체
     * @throws BaseException 읽은 알림 메시지가 존재하지 않을 경우 예외 발생
     */
    @Transactional
    public void deleteNotificationIsReadTrue(User user) {
        // 사용자의 읽은 모든 알림 조회
        List<Notification> notificationList = notificationRepository.findAllByReceiverAndIsReadTrue(user);
        log.info(notificationList.toString());

        // 읽은 알림이 없으면 예외 발생
        if (notificationList.isEmpty()) {
            throw new BaseException(BaseResponseStatus.READ_NOTIFICATION_LIST_NOT_FOUND);
        }

        // 조회된 읽은 알림들을 전부 삭제
        notificationRepository.deleteAll(notificationList);
    }

    /**
     * 사용자가 지정한 특정 알림 메시지를 삭제합니다. 사용자가 해당 알림의 수신자인 경우에만 삭제가 가능합니다.
     *
     * @param user 알림을 삭제하려는 사용자 객체
     * @param notificationId 삭제할 알림의 ID
     * @throws BaseException 알림이 존재하지 않거나, 사용자가 알림의 수신자가 아닐 경우 예외 발생
     */
    @Transactional
    public void deleteNotification(User user, Long notificationId) {
        // 알림 ID로 알림 객체 조회, 없으면 예외 발생
        Notification notification = notificationRepository.findById(notificationId).orElseThrow(
                () -> new BaseException(BaseResponseStatus.NOTIFICATION_NOT_FOUND));

        // 요청한 사용자가 알림의 수신자가 아니면 권한 예외 발생
        if (!notification.getReceiver().getId().equals(user.getId())) {
            throw new BaseException(BaseResponseStatus.UNAUTHORIZED_DELETE_NOTIFICATION);
        }

        // 조건을 만족하는 경우 해당 알림 삭제
        notificationRepository.delete(notification);
    }
}
