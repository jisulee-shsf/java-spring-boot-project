package com.giftforyoube.notification.repository;

import com.giftforyoube.notification.entity.Notification;
import com.giftforyoube.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByReceiverOrderByCreatedAtDesc(User user);

    List<Notification> findAllByReceiver(User user);
}
