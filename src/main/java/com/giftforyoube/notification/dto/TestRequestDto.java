package com.giftforyoube.notification.dto;

import com.giftforyoube.notification.entity.NotificationType;
import com.giftforyoube.notification.entity.RelatedUrl;
import com.giftforyoube.user.entity.User;
import jakarta.persistence.Id;

public class TestRequestDto {

    private NotificationType notificationType;
    private RelatedUrl url;
}
