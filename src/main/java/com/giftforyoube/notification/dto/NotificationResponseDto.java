package com.giftforyoube.notification.dto;

import com.giftforyoube.notification.entity.Notification;
import com.giftforyoube.notification.entity.NotificationType;
import com.giftforyoube.notification.entity.RelatedUrl;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class NotificationResponseDto {
    private Long notificationId;
    private String content;
    private String url;
    private NotificationType notificationType;
    private Boolean isRead;
    private LocalDateTime createdAt;
    public NotificationResponseDto(Long id, String content, RelatedUrl url,
                                   NotificationType notificationType, Boolean isRead, LocalDateTime createdAt) {
        this.notificationId = id;
        this.content = content;
        this.url = url.getUrl();
        this.notificationType = notificationType;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    public NotificationResponseDto(Notification notification) {
        this.notificationId = notification.getId();
        this.content = notification.getContent();
        this.url = notification.getUrl().getUrl();
        this.notificationType = notification.getNotificationType();
        this.isRead = notification.getIsRead();
        this.createdAt = notification.getCreatedAt();
    }
}
