package com.giftforyoube.notification.controller;

import com.giftforyoube.global.security.UserDetailsImpl;
import com.giftforyoube.notification.dto.NotificationResponseDto;
import com.giftforyoube.notification.service.NotificationService;
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
public class NotificationController {
    private final NotificationService notificationService;

    // 사용자 SSE 연결 API
    @GetMapping(value = "/subscribe", produces = "text/event-stream")
    public ResponseEntity<SseEmitter> sseConnect(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                 @RequestHeader(value = "Last-Event-ID", required = false, defaultValue = "") String lastEventId,
                                                 HttpServletResponse response) {
        return new ResponseEntity<>(notificationService.subscribeAlarm(userDetails.getUsername(),lastEventId, response), HttpStatus.OK);
    }

    // 전체 알림 조회 API
    @GetMapping
    public ResponseEntity<List<NotificationResponseDto>> getNotifications(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return new ResponseEntity<>(notificationService.getNotifications(userDetails.getUser()), HttpStatus.OK);
    }

    // 해당 알림 조회 시 읽음 처리 API
    @PutMapping("/{notificationId}")
    public NotificationResponseDto readNotification(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                 @PathVariable Long notificationId) {
        return notificationService.readNotification(userDetails.getUser(), notificationId);
    }


}
