package com.giftforyoube.notification.controller;

import com.giftforyoube.global.exception.BaseException;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.global.security.UserDetailsImpl;
import com.giftforyoube.notification.dto.NotificationResponseDto;
import com.giftforyoube.notification.entity.NotificationType;
import com.giftforyoube.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "알림", description = "알림 관련 API")
public class NotificationController {
    private final NotificationService notificationService;

    /**
     * SSE 연결을 설정하여 클라이언트가 서버로부터 실시간 업데이트를 받을 수 있도록 합니다.
     * 클라이언트는 이 엔드포인트를 호출하여 SSE 연결을 초기화하고, 서버로부터 실시간 알림을 수신할 수 있습니다.
     *
     * @param userDetails 인증된 사용자의 세부 정보. Spring Security의 @AuthenticationPrincipal을 통해 자동 주입됩니다.
     * @param lastEventId 클라이언트가 마지막으로 수신한 이벤트 ID. 누락된 이벤트를 처리하기 위해 사용됩니다.
     * @param response HttpServletResponse 객체. SSE 연결 설정에 필요한 헤더를 설정하는 데 사용됩니다.
     * @return SseEmitter 객체를 포함하는 ResponseEntity. 클라이언트는 이 객체를 통해 서버로부터 이벤트를 수신합니다.
     */
    @GetMapping(value = "/subscribe", produces = "text/event-stream")
    public ResponseEntity<SseEmitter> sseConnect(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                 @RequestHeader(value = "Last-Event-ID", required = false, defaultValue = "") String lastEventId,
                                                 HttpServletResponse response) {
        return new ResponseEntity<>(notificationService.sseSubscribe(userDetails.getUsername(),lastEventId, response), HttpStatus.OK);
    }

    /**
     * 인증된 사용자의 모든 알림을 조회합니다.
     *
     * @param userDetails 인증된 사용자의 세부 정보. Spring Security에서 자동 주입됩니다.
     * @return 사용자의 모든 알림에 대한 리스트를 담은 ResponseEntity.
     * @throws BaseException 사용자 세부 정보가 존재하지 않을 경우 예외 발생.
     */
    @GetMapping
    public ResponseEntity<List<NotificationResponseDto>> getNotifications(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            throw new BaseException(BaseResponseStatus.NOT_FOUND_USERDETAILS);
        }
        return new ResponseEntity<>(notificationService.getNotifications(userDetails.getUser()), HttpStatus.OK);
    }

    /**
     * 지정된 알림을 읽음 처리합니다. 이 작업은 알림을 조회하는 동시에 수행됩니다.
     *
     * @param userDetails 인증된 사용자의 세부 정보.
     * @param notificationId 읽음 처리할 알림의 ID.
     * @return 읽음 처리된 알림에 대한 ResponseEntity.
     * @throws BaseException 사용자 세부 정보가 존재하지 않을 경우 예외 발생.
     */
    @PatchMapping("/{notificationId}")
    public ResponseEntity<NotificationResponseDto> readNotification(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                 @PathVariable Long notificationId) {
        if (userDetails == null) {
            throw new BaseException(BaseResponseStatus.NOT_FOUND_USERDETAILS);
        }
        return new ResponseEntity<>(notificationService.readNotification(userDetails.getUser(), notificationId), HttpStatus.OK);
    }

    /**
     * 사용자가 읽은 모든 알림 메세지를 삭제합니다.
     *
     * @param userDetails 인증된 사용자의 세부 정보.
     * @return 삭제 성공 메시지를 담은 ResponseEntity.
     * @throws BaseException 사용자 세부 정보가 존재하지 않을 경우 예외 발생.
     */
    @DeleteMapping
    public ResponseEntity<?> deleteNotificationIsReadTrue(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            throw new BaseException(BaseResponseStatus.NOT_FOUND_USERDETAILS);
        }
        notificationService.deleteNotificationIsReadTrue(userDetails.getUser());
        return ResponseEntity.ok().body("읽은 모든 알림 메세지를 성공적으로 삭제하였습니다.");
    }

    /**
     * 사용자가 지정한 알림 메세지를 삭제합니다.
     *
     * @param userDetails 인증된 사용자의 세부 정보.
     * @param notificationId 삭제할 알림의 ID.
     * @return 삭제 성공 메시지를 담은 ResponseEntity.
     * @throws BaseException 사용자 세부 정보가 존재하지 않을 경우 예외 발생.
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<?> deleteNotification(@AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable Long notificationId) {
        if (userDetails == null) {
            throw new BaseException(BaseResponseStatus.NOT_FOUND_USERDETAILS);
        }
        notificationService.deleteNotification(userDetails.getUser(), notificationId);
        return ResponseEntity.ok().body("해당 알림 메세지를 성공적으로 삭제하였습니다.");
    }

    ////////////////////////////////////////////////////////////////////////

    // 알림메시지 테스트
    @PostMapping("/test")
    public ResponseEntity<String> NotificationTest(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        String testContent = "테스트 알림입니다!";
        String testUrl = "https://giftipie.me";
        NotificationType testNotificationType = NotificationType.DONATION;
        notificationService.send(userDetails.getUser(), testNotificationType, testContent, testUrl);

        String responseText = "알림메시지 발송 성공";
        return new ResponseEntity<>(responseText, HttpStatus.OK);
    }
}
