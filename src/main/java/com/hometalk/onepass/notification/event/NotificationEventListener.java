package com.hometalk.onepass.notification.event;

import com.hometalk.onepass.notification.dto.NotificationResponse;
import com.hometalk.onepass.notification.entity.Notification;
import com.hometalk.onepass.notification.service.NotificationService;
import com.hometalk.onepass.notification.service.SseEmitterManager;

import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService  notificationService;
    private final SseEmitterManager    sseEmitterManager;

    // @TransactionalEventListener: DB 커밋 성공 후에만 실행
    // @Async: SSE 전송 실패가 메인 트랜잭션에 영향 없음
    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationEvent(NotificationEvent event) {
        Notification saved = notificationService.save(event);

        // 중복 skip된 경우 SSE 전송 안 함
        if (saved == null) return;

        NotificationResponse response = new NotificationResponse(
                saved.getId(), saved.getType(), saved.getTitle(),
                saved.getMessage(), saved.getLink(), false, saved.getCreatedAt()
        );

        if (event.getUserId() == null) {
            sseEmitterManager.sendToAll(event.getTargetRole(), response);
        } else {
            sseEmitterManager.sendToUser(event.getUserId(), response);
        }
    }
}