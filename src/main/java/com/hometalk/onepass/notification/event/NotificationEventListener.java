package com.hometalk.onepass.notification.event;

import com.hometalk.onepass.notification.dto.NotificationResponse;
import com.hometalk.onepass.notification.entity.Notification;
import com.hometalk.onepass.notification.entity.NotificationType;
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

    private final NotificationService notificationService;
    private final SseEmitterManager   sseEmitterManager;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAfterCommit(NotificationEvent event) {
        sendNotification(event);
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleAfterRollback(NotificationEvent event) {
        if (event.getType() != NotificationType.VEHICLE_TICKET_SHORTAGE) return;
        sendNotification(event);
    }

    private void sendNotification(NotificationEvent event) {
        Notification saved = notificationService.save(event);
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