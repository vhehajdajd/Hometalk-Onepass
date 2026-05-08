package com.hometalk.onepass.notification.repository;

import com.hometalk.onepass.notification.dto.NotificationResponse;
import com.hometalk.onepass.notification.entity.Notification;
import com.hometalk.onepass.notification.entity.NotificationTargetRole;
import com.hometalk.onepass.notification.entity.NotificationType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // ─────────────── 조회 (DTO Projection) ───────────────

    @Query("""
        SELECT new com.hometalk.onepass.notification.dto.NotificationResponse(
            n.id,
            n.type,
            n.title,
            n.message,
            n.link,
            CASE WHEN nr.id IS NOT NULL THEN true ELSE false END,
            n.createdAt
        )
        FROM Notification n
        LEFT JOIN NotificationRead nr
          ON nr.notification = n AND nr.userId = :userId
        WHERE (n.userId = :userId OR (n.userId IS NULL AND n.targetRole = :role))
        AND   (n.expiredAt IS NULL OR n.expiredAt > CURRENT_TIMESTAMP)
        ORDER BY n.updatedAt DESC
    """)
    Page<NotificationResponse> findAllMyNotificationsWithRead(
            @Param("userId") Long userId,
            @Param("role")   NotificationTargetRole role,
            Pageable pageable
    );

    // ─────────────── 미읽음 수 ───────────────

    @Query("""
        SELECT COUNT(n) FROM Notification n
        WHERE (n.userId = :userId OR (n.userId IS NULL AND n.targetRole = :role))
        AND   n.id NOT IN (
            SELECT nr.notification.id FROM NotificationRead nr WHERE nr.userId = :userId
        )
        AND (n.expiredAt IS NULL OR n.expiredAt > CURRENT_TIMESTAMP)
    """)
    long countUnreadNotifications(
            @Param("userId") Long userId,
            @Param("role")   NotificationTargetRole role
    );

    // ─────────────── 중복방지 / Upsert ───────────────

    boolean existsByTypeAndUserIdAndReferenceId(
            NotificationType type, Long userId, Long referenceId
    );

    Optional<Notification> findByTypeAndUserIdAndReferenceId(
            NotificationType type, Long userId, Long referenceId
    );

    @Modifying
    @Query("""
        UPDATE Notification n
        SET   n.title = :title,
              n.message = :message,
              n.link = :link,
              n.expiredAt = :expiredAt
        WHERE n.type = :type
        AND   n.userId = :userId
        AND   n.referenceId = :referenceId
    """)
    int upsertByReferenceId(
            @Param("type") NotificationType type,
            @Param("userId") Long userId,
            @Param("referenceId") Long referenceId,
            @Param("title") String title,
            @Param("message") String message,
            @Param("link") String link,
            @Param("expiredAt") LocalDateTime expiredAt
    );

    @Query("""
        SELECT COUNT(n) > 0 FROM Notification n
        WHERE n.userId IS NULL
        AND   n.targetRole = :role
        AND   n.type = :type
        AND   n.title = :title
        AND   n.createdAt > :since
    """)
    boolean existsRecentBroadcast(
            @Param("role")  NotificationTargetRole role,
            @Param("type")  NotificationType type,
            @Param("title") String title,
            @Param("since") LocalDateTime since
    );

    // ─────────────── 전체발송 알림 조회 (markBroadcastAsRead용) ───────────────

    List<Notification> findByTypeAndUserIdIsNullAndTargetRole(
            NotificationType type, NotificationTargetRole targetRole
    );

    boolean existsByTypeAndUserId(NotificationType type, Long userId);

    // 관리비 납부완료 처리 시 삭제 대상 알림 ID 목록 조회 (notification_read 선삭제용)
    @Query("SELECT n.id FROM Notification n WHERE n.type = :type AND n.userId = :userId")
    List<Long> findIdsByTypeAndUserId(@Param("type") NotificationType type,
                                      @Param("userId") Long userId);

    // ─────────────── 삭제 ───────────────

    @Modifying
    void deleteByTypeAndUserIdAndReferenceId(
            NotificationType type, Long userId, Long referenceId
    );

    @Modifying
    void deleteByTypeAndUserId(NotificationType type, Long userId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.expiredAt IS NOT NULL AND n.expiredAt < CURRENT_TIMESTAMP")
    int deleteExpiredNotifications();
}