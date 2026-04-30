import java.time.LocalDateTime;

@Entity
@Table(name = "notification", indexes = {
    @Index(name = "idx_noti_user_role", columnList = "user_id, target_role"),
    @Index(name = "idx_noti_expired",   columnList = "expired_at"),
    @Index(name = "idx_noti_updated",   columnList = "updated_at"),
    @Index(name = "idx_noti_reference", columnList = "reference_id")
})
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;            // null = 전체 발송
    private Long reference_id;      // 식별용 (null 허용)

    @Enumerated(EnumType.STRING)
    private NotificationTargetRole targetRole;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private String title;
    private String message;
    private String link;       // null 허용

    private LocalDateTime expiredAt;
}