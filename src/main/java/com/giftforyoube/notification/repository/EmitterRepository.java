package com.giftforyoube.notification.repository;

import com.giftforyoube.notification.entity.Notification;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

public interface EmitterRepository {

    // emitter 저장
    SseEmitter save(String emitterId, SseEmitter sseEmitter);

    void deleteAllEmitterStartWithId(String emitterId);

    void deleteAllEventCacheStartWithId(String userId);

    void deleteById(String emitterId);

    Map<String, Object> findAllEventCacheStartWithByUserId(String userId);

    Map<String, SseEmitter> findAllEmitterStartWithByUserId(String receiverId);

    void saveEventCache(String emitterId, Object event);
}
