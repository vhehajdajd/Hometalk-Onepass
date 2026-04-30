import java.time.LocalDateTime;

import dto.NotificationResponse;
import entity.NotificationTargetRole;
import entity.NotificationType;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // ✅ isRead 포함 목록 조회 (LEFT JOIN → DTO Projection)
    // ORDER BY updatedAt DESC: Upsert 시 갱신된 알림이 상단 노출
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

    // 미읽음 수 (전체발송 포함)
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


    // BILLING_PAID 즉시삭제
    void deleteByTypeAndUserIdAndBillingId(
        NotificationType type, Long userId, Long billingId
    );

    // 배치 만료 삭제 (매일 03:00)
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.expiredAt IS NOT NULL AND n.expiredAt < CURRENT_TIMESTAMP")
    int deleteExpiredNotifications();

    // BILLING 중복 방지: 동일 (type, userId, billingId)는 항상 1건만 유지. UPDATE Upsert.
    boolean existsByTypeAndUserIdAndBillingId(
        NotificationType type, Long userId, Long billingId
    );

    // BILLING 중복 방지: billingId = null 케이스 (전체 발송 알림 — type 기준)
    // 최근 1시간 내 동일 type + title 알림 존재 여부
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

    // BILLING 호출 예시: existsByTypeAndUserIdIsNullAndTargetRole(BILLING_UPLOAD, RESIDENT)
    boolean existsByTypeAndUserIdIsNullAndTargetRole(
        NotificationType type, NotificationTargetRole targetRole
    );
    // BILLING Upsert 처리 — UPDATE 방식 (id 유지)
    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.title = :title,
            n.message = :message,
            n.link = :link,
            n.expiredAt = :expiredAt,
            n.updatedAt = CURRENT_TIMESTAMP
        WHERE n.type = :type
        AND   n.userId = :userId
        AND   n.billingId = :billingId
    """)
    int upsertByBillingId(
        @Param("type") NotificationType type,
        @Param("userId") Long userId,
        @Param("billingId") Long billingId,
        @Param("title") String title,
        @Param("message") String message,
        @Param("link") String link,
        @Param("expiredAt") LocalDateTime expiredAt
    );

    
}