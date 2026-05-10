package com.hometalk.onepass.notification.publisher;

import com.hometalk.onepass.notification.entity.NotificationTargetRole;
import com.hometalk.onepass.notification.entity.NotificationType;
import com.hometalk.onepass.notification.event.NotificationEvent;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /* 특정 사용자에게 알림 발행
        @return void - 알림 발행 실패가 비즈니스 트랜잭션에 영향을 주어서는 안되므로, 발행 성공 여부를 반환하지 않고 void로 처리함
                        (발행 실패는 이벤트 리스너에서 예외처리/로그기록을 통해 추적가능, 비즈니스 로직에 영향 없음)
     */
    public void publish(Long userId,
                        NotificationTargetRole role,
                        NotificationType type,
                        String title,
                        String message,
                        String link,
                        Long referenceId) {
        eventPublisher.publishEvent(
                new NotificationEvent(userId, role, type, title, message, link, referenceId)
        );
    }

    /**
     * 전체 발송 (userId = null)
     * referenceId = null로 고정
     */
    public void publishToAll(NotificationTargetRole role,
                             NotificationType type,
                             String title,
                             String message,
                             String link) {
        eventPublisher.publishEvent(
                new NotificationEvent(null, role, type, title, message, link, null)
        );
    }
}