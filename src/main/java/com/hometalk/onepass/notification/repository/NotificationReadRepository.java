package com.hometalk.onepass.notification.repository;

import com.hometalk.onepass.notification.entity.NotificationRead;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificationReadRepository extends JpaRepository<NotificationRead, Long> {

    /**
     * 단건 읽음 처리 시 중복 INSERT 방지용 조회
     */
    Optional<NotificationRead> findByNotificationIdAndUserId(Long notificationId, Long userId);

    // 알림 삭제 전 연관된 읽음 기록 일괄 삭제 (FK 제약 해제용)
    void deleteByNotificationIdIn(List<Long> notificationIds);

    /**
     * 전체 읽음 처리 (Native Bulk INSERT)
     * ⚠️ 호출 시 반드시 role.name() 전달 (String 파라미터)
     * 예: bulkReadAll(userId, role.name())
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
        INSERT INTO notification_read (notification_id, user_id, read_at)
        SELECT n.id, :userId, NOW()
        FROM notification n
        WHERE (n.user_id = :userId OR (n.user_id IS NULL AND n.target_role = :role))
        AND n.id NOT IN (
            SELECT nr.notification_id FROM notification_read nr WHERE nr.user_id = :userId
        )
        AND (n.expired_at IS NULL OR n.expired_at > CURRENT_TIMESTAMP)
    """, nativeQuery = true)
    int bulkReadAll(@Param("userId") Long userId, @Param("role") String role);

    /**
     * 특정 알림이 특정 사용자에게 이미 읽음 처리되었는지 확인
     * 사용 케이스: markBroadcastAsRead 호출 시 중복 INSERT 방지
     */
    boolean existsByNotificationIdAndUserId(Long notificationId, Long userId);
}