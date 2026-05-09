package com.hometalk.onepass.notification.service;

import com.hometalk.onepass.notification.entity.NotificationTargetRole;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class SseEmitterManager {

    // ✅ userId → {role, emitters} 구조로 변경 (DB 조회 제거)
    private final Map<Long, UserSession> sessionMap = new ConcurrentHashMap<>();

    private static final long TIMEOUT_MS = 30 * 60 * 1000L;

    // ─────────────────── 내부 세션 클래스 ───────────────────
    private static class UserSession {
        final NotificationTargetRole role;
        final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

        UserSession(NotificationTargetRole role) {
            this.role = role;
        }
    }

    // ─────────────────── 연결 등록 ───────────────────

    public SseEmitter connect(Long userId, NotificationTargetRole role, String lastEventId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);

        // ✅ role과 함께 세션 등록
        sessionMap.computeIfAbsent(userId, k -> new UserSession(role)).emitters.add(emitter);

        Runnable removeEmitter = () -> removeEmitter(userId, emitter);
        emitter.onCompletion(removeEmitter);
        emitter.onTimeout(() -> {
            sendReconnectSignal(emitter);
            removeEmitter.run();
        });
        emitter.onError(e -> removeEmitter.run());

        // Last-Event-ID 처리 (④번에서 구현)
        if (lastEventId != null && !lastEventId.isBlank()) {
            // TODO: ④ Last-Event-ID 기반 누락 알림 재전송
        }

        sendEvent(emitter, "connect", "connected");
        return emitter;
    }

    // ─────────────────── 전송 ───────────────────

    public void sendToUser(Long userId, Object data) {
        UserSession session = sessionMap.get(userId);
        if (session == null) return;
        session.emitters.forEach(emitter -> sendEvent(emitter, "notification", data));
    }

    // ✅ DB 조회 없이 세션에서 role 직접 비교
    public void sendToAll(NotificationTargetRole role, Object data) {
        sessionMap.forEach((userId, session) -> {
            if (session.role == role) {
                session.emitters.forEach(emitter -> sendEvent(emitter, "notification", data));
            }
        });
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

    private void sendReconnectSignal(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().name("reconnect").data("timeout"));
        } catch (IOException ignored) {}
    }

    // ─────────────────── Heartbeat ───────────────────
    @Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        sessionMap.forEach((userId, session) ->
                session.emitters.forEach(emitter -> {
                    try {
                        emitter.send(SseEmitter.event().comment("heartbeat"));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                        removeEmitter(userId, emitter);
                    }
                })
        );
    }

    // ─────────────────── 제거 ───────────────────
    private void removeEmitter(Long userId, SseEmitter emitter) {
        UserSession session = sessionMap.get(userId);
        if (session != null) {
            session.emitters.remove(emitter);
            if (session.emitters.isEmpty()) sessionMap.remove(userId);
        }
    }
}