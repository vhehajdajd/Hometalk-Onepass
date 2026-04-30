@Entity
@Table(name = "notification_read", indexes = {
    @Index(
        name = "idx_noti_read_composite",
        columnList = "notification_id, user_id",
        unique = true   // ← 중복 읽음 처리 DB 레벨 차단
    )
})
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationRead {   // 읽음 이력은 수정 없음 → BaseTimeEntity 불필요

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDateTime readAt;
}