package com.giftforyoube.notification.entity;

import com.giftforyoube.global.entity.Auditable;
import com.giftforyoube.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Notification extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String content;

    @Embedded
    @Column(nullable = false)
    private RelatedUrl url;

    @Column(nullable = false)
    private Boolean isRead;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType notificationType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User receiver;

    @Builder
    public Notification(User receiver, NotificationType notificationType, String content, String url, Boolean isRead) {
        this.receiver = receiver;
        this.notificationType = notificationType;
        this.content = content;
        this.url = new RelatedUrl(url);
        this.isRead = isRead;
    }

    public void setIsRead(boolean isRead) {
        this.isRead = isRead;
    }

    ////////////////////////////////
//    // test용

    public void setId(Long id) {
        this.id = id;
    }
//
//    @Builder
//    public Notification(Long notificationId, User receiver, NotificationType notificationType, String content, String url, Boolean isRead) {
//        this.id = notificationId;
//        this.receiver = receiver;
//        this.notificationType = notificationType;
//        this.content = content;
//        this.url = new RelatedUrl(url);
//        this.isRead = isRead;
//    }
}
