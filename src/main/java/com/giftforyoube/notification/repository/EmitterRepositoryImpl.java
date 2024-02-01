package com.giftforyoube.notification.repository;

import com.giftforyoube.notification.entity.Notification;
import com.giftforyoube.user.entity.User;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class EmitterRepositoryImpl implements EmitterRepository{
    // SseEmitter를 관리하는 스레드들이 콜백할때 스레드가 다를수 있기에 ThreadSafe한 구조인 ConcurrentHashMap을 사용
    // 동시성을 고려하여 ConcurrentHashMap 사용 -> 가능한 많은 클라이언트의 요청을 처리할 수 있도록 함
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Map<String, Object> eventCache = new ConcurrentHashMap<>();

    @Override
    public SseEmitter save(String emitterId, SseEmitter sseEmitter) {
        emitters.put(emitterId, sseEmitter);
        return sseEmitter;
    }

    @Override   // event 저장
    public void saveEventCache(String eventCacheId, Object event) {

        eventCache.put(eventCacheId, event);
    }

    @Override   // 구분자로 회원 ID를 사용하기에 StartWith를 사용 - 회원과 관련된 모든 emitter를 찾는다.
    public Map<String, SseEmitter> findAllEmitterStartWithByUserId(String UserId) {
        return emitters.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(UserId))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    // 회원에게 수신된 모든 이벤트를 찾는다.
    @Override
    public Map<String, Object> findAllEventCacheStartWithByUserId(String UserId) {
        return eventCache.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(UserId))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public void deleteById(String id) {
        emitters.remove(id);
    }

    // 해당 회원과 관련된 모든 emitter를 지움
    @Override
    public void deleteAllEmitterStartWithId(String userId) {
        emitters.forEach(
                (key, emitter) -> {
                    if (key.startsWith(userId)) {
                        emitters.remove(key);
                        deleteAllEventCacheStartWithId(userId);
                    }
                }
        );
    }

    // 해당 회원과 관련된 모든 이벤트를 지움
    @Override
    public void deleteAllEventCacheStartWithId(String userID) {
        eventCache.forEach(
                (key, emitter) -> {
                    if (key.startsWith(userID)) { eventCache.remove(key); }
                }
        );
    }
}
