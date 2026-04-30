import java.time.LocalDateTime;

import dto.NotificationResponse;
import entity.NotificationRead;
import entity.NotificationTargetRole;
import entity.NotificationType;
import event.NotificationEvent;
import repository.NotificationReadRepository;
import repository.NotificationRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository     notificationRepository;
    private final NotificationReadRepository notificationReadRepository;

    // 알림 목록 조회 (isRead 포함, 페이징)
    public Page<NotificationResponse> getNotifications(
        Long userId, NotificationTargetRole role, Pageable pageable
    ) {
        return notificationRepository.findAllMyNotificationsWithRead(userId, role, pageable);
    }

    // 미읽음 수
    public long countUnread(Long userId, NotificationTargetRole role) {
        return notificationRepository.countUnreadNotifications(userId, role);
    }

    // 단건 읽음 처리
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

    // 전체 읽음 처리
    // ⚠️ role.name() 전달 — nativeQuery String 파라미터
    @Transactional
    public void markAllAsRead(Long userId, NotificationTargetRole role) {
        notificationReadRepository.bulkReadAll(userId, role.name());
    }

    // DB 저장 + expiredAt 계산 (EventListener에서 호출)
@Transactional
public Notification save(NotificationEvent event) {
    LocalDateTime expiredAt = calculateExpiredAt(event.getType());

    // CASE 1: billingId 있음 → UPDATE 시도, 없으면 INSERT
    if (event.getBillingId() != null) {
        if (notificationRepository.existsByTypeAndUserIdAndBillingId(
                event.getType(), event.getUserId(), event.getBillingId())) {
            notificationRepository.upsertByBillingId(
                event.getType(), event.getUserId(), event.getBillingId(),
                event.getTitle(), event.getMessage(), event.getLink(), expiredAt
            );
            return notificationRepository
                .findByTypeAndUserIdAndBillingId(
                    event.getType(), event.getUserId(), event.getBillingId())
                .orElseThrow();
        }
    }
    // CASE 2: billingId 없음 (전체 공지) → 최근 1시간 윈도우 체크
    else {
        boolean recentExists = notificationRepository.existsRecentBroadcast(
            event.getTargetRole(), event.getType(), event.getTitle(),
            LocalDateTime.now().minusHours(1)
        );
        if (recentExists) {
            // 1시간 내 동일 알림 → skip (조용히 무시)
            return null;
        }
    }

    // INSERT
    return notificationRepository.save(
        Notification.builder()
            .userId(event.getUserId())
            .billingId(event.getBillingId())
            .targetRole(event.getTargetRole())
            .type(event.getType())
            .title(event.getTitle())
            .message(event.getMessage())
            .link(event.getLink())
            .expiredAt(expiredAt)
            .build()
    );
}

    // 각 모듈 즉시삭제케이스 (납부완료 처리 시 BillingService → NotificationPublisher 경유로 호출)
    @Transactional
    public void deleteByReference(NotificationType type, Long userId, Long referenceId) {
    notificationRepository.deleteByTypeAndUserIdAndReferenceId(type, userId, referenceId);
}

    // 만료일 계산
    private LocalDateTime calculateExpiredAt(NotificationType type) {
        int days = type.getExpireDays();
        if (days == 0)  return null;                         // 영구
        if (days == -1) return null;                         // 즉시삭제 조건부 → 별도 처리
        return LocalDateTime.now().plusDays(days);
    }

    // @Scheduled 배치 삭제 (매일 03:00)
    @Transactional
    @Scheduled(cron = "0 0 3 * * *")
    public void deleteExpiredNotifications() {
        notificationRepository.deleteExpiredNotifications();
    }
}