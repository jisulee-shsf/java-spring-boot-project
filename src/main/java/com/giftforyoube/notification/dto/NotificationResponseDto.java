package com.giftforyoube.notification.dto;

import com.giftforyoube.notification.entity.Notification;
import com.giftforyoube.notification.entity.NotificationType;
import com.giftforyoube.notification.entity.RelatedUrl;
import jakarta.persistence.GeneratedValue;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NotificationResponseDto {
    private Long notificationId;
    private String content;
    private String url;
    private NotificationType notificationType;
    private Boolean isRead;
    public NotificationResponseDto(Long id, String content, RelatedUrl url,
                                   NotificationType notificationType, Boolean isRead) {
        this.notificationId = id;
        this.content = content;
        this.url = url.getUrl();
        this.notificationType = notificationType;
        this.isRead = isRead;
    }
}
