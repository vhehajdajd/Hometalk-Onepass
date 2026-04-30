package com.hometalk.onepass.notification.service;

import com.hometalk.onepass.notification.entity.NotificationTargetRole;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@RequiredArgsConstructor
public class SseEmitterManager {

    // 동시성 제어: ConcurrentHashMap + CopyOnWriteArrayList
    private final Map<Long, List<SseEmitter>> emitterMap = new ConcurrentHashMap<>();

    private static final long TIMEOUT_MS    = 30 * 60 * 1000L; // 30분
    private static final long HEARTBEAT_MS  = 30 * 1000L;      // 30초

    // ─────────────────── 연결 등록 ───────────────────

    public SseEmitter connect(Long userId, String lastEventId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);

        emitterMap.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // 생명 주기 콜백: 만료/오류 시 맵에서 즉시 제거
        Runnable removeEmitter = () -> removeEmitter(userId, emitter);
        emitter.onCompletion(removeEmitter);
        emitter.onTimeout(() -> {
            sendReconnectSignal(emitter);   // 클라이언트에 재연결 시그널 전송
            removeEmitter.run();
        });
        emitter.onError(e -> removeEmitter.run());

        // Last-Event-ID 처리: 재연결 시 누락 알림 flush
        if (lastEventId != null && !lastEventId.isBlank()) {
            // TODO: Security 연동 후 userId + targetRole 기반 미전송 알림 재조회 후 전송
            // notificationService.findAfter(userId, role, lastEventId)
            //     .forEach(n -> send(emitter, n));
        }

        // 최초 연결 확인용 더미 이벤트
        sendEvent(emitter, "connect", "connected");
        return emitter;
    }

    // ─────────────────── 전송 ───────────────────

    public void sendToUser(Long userId, Object data) {
        List<SseEmitter> emitters = emitterMap.getOrDefault(userId, List.of());
        emitters.forEach(emitter -> sendEvent(emitter, "notification", data));
    }

    public void sendToAll(NotificationTargetRole role, Object data) {
        // 현재 접속 중인 모든 emitter에 전체 발송
        // Security 연동 후 role 필터링 추가 예정
        emitterMap.values().forEach(list ->
                list.forEach(emitter -> sendEvent(emitter, "notification", data))
        );
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(
                    SseEmitter.event()
                            .name(eventName)
                            .data(data, MediaType.APPLICATION_JSON)
                            .id(String.valueOf(System.currentTimeMillis()))
            );
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    // 클라이언트에 재연결 시그널 전송 후 종료
    private void sendReconnectSignal(SseEmitter emitter) {
        try {
            emitter.send(
                    SseEmitter.event().name("reconnect").data("timeout")
            );
        } catch (IOException ignored) { }
    }

    // ─────────────────── Heartbeat ───────────────────
    // SseEmitterManager 내부 @Scheduled: 30초마다 빈 comment 전송
    @Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        emitterMap.values().forEach(list ->
                list.forEach(emitter -> {
                    try {
                        emitter.send(SseEmitter.event().comment("heartbeat"));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                })
        );
    }

    // ─────────────────── 제거 ───────────────────

    private void removeEmitter(Long userId, SseEmitter emitter) {
        List<SseEmitter> list = emitterMap.get(userId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) emitterMap.remove(userId);
        }
    }
}