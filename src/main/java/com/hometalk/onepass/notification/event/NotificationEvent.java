// 이벤트 구조 명세

@Getter
@RequiredArgsConstructor
public class NotificationEvent {
    private final Long                   userId;      // null = 전체 발송
    private final NotificationTargetRole targetRole;
    private final NotificationType       type;
    private final String                 title;
    private final String                 message;
    private final String                 link;        // null 허용
    private final Long                   billingId;   // null 허용 (관리비 관련 시만 사용)
}