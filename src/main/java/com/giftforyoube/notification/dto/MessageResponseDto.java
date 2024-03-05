package com.giftforyoube.notification.dto;

import com.giftforyoube.notification.entity.Notification;
import com.giftforyoube.notification.entity.NotificationType;
import com.giftforyoube.notification.entity.RelatedUrl;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class MessageResponseDto {
    private Long notificationId;
    private String message;
    private String url;
    private NotificationType notificationType;
    private Boolean isRead;
    private LocalDateTime createdAt;
    public MessageResponseDto(Long id, String content, RelatedUrl url,
                                   NotificationType notificationType, Boolean isRead, LocalDateTime createdAt) {
        this.notificationId = id;
        this.message = content;
        this.url = url.getUrl();
        this.notificationType = notificationType;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    public MessageResponseDto(Notification notification) {
        this.notificationId = notification.getId();
        this.message = notification.getContent();
        this.url = notification.getUrl().getUrl();
        this.notificationType = notification.getNotificationType();
        this.isRead = notification.getIsRead();
        this.createdAt = notification.getCreatedAt();
    }
}