public interface NotificationReadRepository extends JpaRepository<NotificationRead, Long> {

    // 전체 읽음 처리 (Native Bulk INSERT)
    // ⚠️ 호출 시 반드시 role.name() 전달 (String 파라미터이므로 Enum 직접 전달 금지)
    // 예: bulkReadAll(userId, role.name())
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
    // ↑ nativeQuery이므로 role은 String. 반드시 role.name()으로 호출할 것
}