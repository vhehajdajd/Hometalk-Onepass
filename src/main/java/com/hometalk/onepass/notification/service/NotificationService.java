package com.hometalk.onepass.notification.service;

import com.hometalk.onepass.notification.dto.NotificationResponse;
import com.hometalk.onepass.notification.entity.Notification;
import com.hometalk.onepass.notification.entity.NotificationRead;
import com.hometalk.onepass.notification.entity.NotificationTargetRole;
import com.hometalk.onepass.notification.entity.NotificationType;
import com.hometalk.onepass.notification.event.NotificationEvent;
import com.hometalk.onepass.notification.repository.NotificationRepository;
import com.hometalk.onepass.notification.repository.NotificationReadRepository;

import jakarta.persistence.EntityNotFoundException;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationReadRepository notificationReadRepository;

    // ─────────────────── 조회 ───────────────────

    public Page<NotificationResponse> getNotifications(
            Long userId, NotificationTargetRole role, Pageable pageable
    ) {
        return notificationRepository.findAllMyNotificationsWithRead(userId, role, pageable);
    }

    public long countUnread(Long userId, NotificationTargetRole role) {
        return notificationRepository.countUnreadNotifications(userId, role);
    }

    // ─────────────────── 읽음 처리 ───────────────────

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        notificationReadRepository.findByNotificationIdAndUserId(notificationId, userId)
                .ifPresentOrElse(
                        r -> { /* 이미 읽음 처리됨 — 무시 */ },
                        () -> {
                            Notification n = notificationRepository.findById(notificationId)
                                    .orElseThrow(() -> new EntityNotFoundException("알림을 찾을 수 없습니다."));
                            notificationReadRepository.save(
                                    NotificationRead.builder()
                                            .notification(n)
                                            .userId(userId)
                                            .readAt(LocalDateTime.now())
                                            .build()
                            );
                        }
                );
    }

    // ─────────────────── 전체삭제 버튼 클릭 -> 전체읽음+삭제 ───────────────────
    @Transactional
    public void deleteAll(Long userId, NotificationTargetRole role) {
        // 읽음 이력 먼저 삭제
        List<Long> notificationIds = notificationRepository
                .findAllMyNotificationIds(userId, role);
        if (!notificationIds.isEmpty()) {
            notificationReadRepository.deleteByNotificationIdIn(notificationIds);
            notificationRepository.deleteAllByIdIn(notificationIds);
        }
    }

    @Transactional
    public void markAllAsRead(Long userId, NotificationTargetRole role) {
        notificationReadRepository.bulkReadAll(userId, role.name());
    }

    /**
     * 전체발송(userId=null) 알림을 특정 사용자가 "읽음" 처리
     * 사용 케이스: 납부완료 시 BILLING_UPLOAD 본인 화면에서만 사라지게
     */
    @Transactional
    public void markBroadcastAsRead(NotificationType type, Long userId, NotificationTargetRole role) {
        List<Notification> broadcasts = notificationRepository
                .findByTypeAndUserIdIsNullAndTargetRole(type, role);

        for (Notification n : broadcasts) {
            if (!notificationReadRepository.existsByNotificationIdAndUserId(n.getId(), userId)) {
                notificationReadRepository.save(
                        NotificationRead.builder()
                                .notification(n)
                                .userId(userId)
                                .readAt(LocalDateTime.now())
                                .build()
                );
            }
        }
    }

    // ─────────────────── 저장 ───────────────────

    /**
     * DB 저장 + expiredAt 계산 (EventListener에서 호출)
     */
    @Transactional
    public Notification save(NotificationEvent event) {
        LocalDateTime expiredAt = calculateExpiredAt(event.getType());

        // CASE 1: referenceId 있음 → 기존 알림 있으면 UPDATE, 없으면 INSERT
        if (event.getReferenceId() != null) {
            if (notificationRepository.existsByTypeAndUserIdAndReferenceId(
                    event.getType(), event.getUserId(), event.getReferenceId())) {

                notificationRepository.upsertByReferenceId(
                        event.getType(), event.getUserId(), event.getReferenceId(),
                        event.getTitle(), event.getMessage(), event.getLink(), expiredAt
                );
                return notificationRepository
                        .findByTypeAndUserIdAndReferenceId(
                                event.getType(), event.getUserId(), event.getReferenceId())
                        .orElseThrow();
            }
        }
        // CASE 2: referenceId 없음 (전체 공지) → 시간 윈도우 체크
        else {
            int windowHours = event.getType().getDedupeWindowHours();
            if (windowHours > 0) {
                boolean recentExists = notificationRepository.existsRecentBroadcast(
                        event.getTargetRole(), event.getType(), event.getTitle(),
                        LocalDateTime.now().minusHours(windowHours)
                );
                if (recentExists) return null; // skip
            }
        }

        // INSERT
        return notificationRepository.save(
                Notification.builder()
                        .userId(event.getUserId())
                        .referenceId(event.getReferenceId())
                        .targetRole(event.getTargetRole())
                        .type(event.getType())
                        .title(event.getTitle())
                        .message(event.getMessage())
                        .link(event.getLink())
                        .expiredAt(expiredAt)
                        .build()
        );
    }

    // ─────────────────── 삭제 ───────────────────

    /**
     * 즉시삭제: 특정 사용자의 특정 type + referenceId 알림 삭제
     * 사용 케이스: 납부완료 시 BILLING_PAID 즉시 삭제 등
     */
    @Transactional
    public void deleteByReference(NotificationType type, Long userId, Long referenceId) {
        notificationRepository.deleteByTypeAndUserIdAndReferenceId(type, userId, referenceId);
    }

    /**
     * 즉시삭제: 특정 사용자의 특정 type 알림 모두 삭제
     * 사용 케이스: 납부완료 시 BILLING_UNPAID / BILLING_OVERDUE 제거
     */
    @Transactional
    public void deleteByTypeAndUser(NotificationType type, Long userId) {
        // 1. notification_read 먼저 삭제 (FK 제약 해제)
        List<Long> notificationIds = notificationRepository
                .findIdsByTypeAndUserId(type, userId);
        if (!notificationIds.isEmpty()) {
            notificationReadRepository.deleteByNotificationIdIn(notificationIds);
        }
        // 2. notification 삭제
        notificationRepository.deleteByTypeAndUserId(type, userId);
    }

    // ─────────────────── 만료/배치 ───────────────────

    private LocalDateTime calculateExpiredAt(NotificationType type) {
        int days = type.getExpireDays();
        if (days <= 0) return null;             // 0 또는 음수 = 영구 (조건부 삭제)
        return LocalDateTime.now().plusDays(days);
    }

        @Transactional
        @Scheduled(cron = "0 0 3 * * *")
        public void deleteExpiredNotifications() {
            notificationRepository.deleteExpiredNotifications();
    }
}