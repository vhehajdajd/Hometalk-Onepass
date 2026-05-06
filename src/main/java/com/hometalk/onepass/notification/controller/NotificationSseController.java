package com.hometalk.onepass.notification.controller;

import com.hometalk.onepass.notification.entity.NotificationTargetRole;
import com.hometalk.onepass.notification.service.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 연결 전용 컨트롤러
 *
 * 엔드포인트:
 *   GET /api/notification/subscribe - SSE 구독 (text/event-stream)
 *
 * 클라이언트는 EventSource로 이 엔드포인트에 연결하여
 * 실시간 알림을 푸시 형태로 수신합니다.
 */
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationSseController {

    private final SseEmitterManager sseEmitterManager;

    /**
     * SSE 구독 연결
     *
     * @param lastEventId 재연결 시 마지막으로 수신한 이벤트 ID (선택)
     *                    Security 연동 후 누락 알림 재전송 로직에서 사용
     * @return SseEmitter (SSE 연결 객체)
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId
            // TODO: Security 연동 후 @AuthenticationPrincipal CustomUserDetails user 추가
    ) {
        // TODO: Security 연동 후 user.getId() 사용
        Long userId = getCurrentUserId();

        return sseEmitterManager.connect(userId, lastEventId);
    }

    // ─────────────────── 임시 사용자 정보 (Security 연동 전) ───────────────────
    // TODO: Security 연동 완료되면 삭제
    private Long getCurrentUserId() {
        return 2L; // 임시 하드코딩
    }
}