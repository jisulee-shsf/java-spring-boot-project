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

    // CocurrentHashMap 을 이용한 인메모리 방식을 우선 사용하였는데, mysql이나 redis같은 db를 왜 활용하지않나?
    // 1. mysql이나 redis에 비해 훨씬 빠르다.
    // SSE는 거의 실시간 통신을 처리하기 때문에 최소한의 지연시간으로 데이터를 저장하고 검색할 수 있는 방법이 좋다.
    // 2. 자원제한 문제에서 벗어나고 확장성 및 관리 용이성
    // -> 어플리케이션 재시작시 데이터가 손실되지않나? (서버 리부팅, 업데이트 배포 시에)
    // -> 데이터의 지속성에는 Redis가 concurrenthashmap보다 낫다.
    // 테스트단계에서는 괜찮지만 비용측면 고려하여 개발 후기단계에서 redis로 변경 고려
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
