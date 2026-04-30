@Getter
public class NotificationResponse {

    private final Long    id;
    private final String  type;
    private final String  category;
    private final String  icon;
    private final String  title;
    private final String  message;
    private final String  link;
    private final Boolean isRead;
    private final String  createdAt;

    // ✅ JPQL new 생성자 — Entity가 아닌 개별 필드 전달 (표준 JPA)
    // category, icon은 type Enum으로부터 내부 파생
    public NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        String link,
        Boolean isRead,
        LocalDateTime createdAt
    ) {
        this.id        = id;
        this.type      = type.name();
        this.category  = type.getCategory();
        this.icon      = type.getIcon();
        this.title     = title;
        this.message   = message;
        this.link      = link;
        this.isRead    = isRead != null && isRead;
        this.createdAt = createdAt.toString();
    }
}