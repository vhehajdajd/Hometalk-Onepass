package com.hometalk.onepass.notification.publisher;

import com.hometalk.onepass.notification.entity.NotificationTargetRole;
import com.hometalk.onepass.notification.entity.NotificationType;
import com.hometalk.onepass.notification.event.NotificationEvent;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
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

    // ✅ 롤백 영향 없이 즉시 발행 (예외 발생 전 알림 발송용 - 주차티켓부족 알림)
    @Async("notificationExecutor")
    @Transactional
    public void publishAsync(Long userId,
                             NotificationTargetRole role,
                             NotificationType type,
                             String title,
                             String message,
                             String link,
                             Long referenceId) {
        try {
            log.info("[publishAsync] 호출 - userId:{}, type:{}", userId, type); // ✅ 추가
            eventPublisher.publishEvent(
                    new NotificationEvent(userId, role, type, title, message, link, referenceId)
            );
            log.info("[publishAsync] 이벤트 발행 완료"); // ✅ 추가
        } catch (Exception e) {
            log.error("[publishAsync] 오류 발생", e); // ✅ 추가
        }
    }
}