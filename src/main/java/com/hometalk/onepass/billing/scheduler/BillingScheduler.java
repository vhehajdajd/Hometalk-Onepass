package com.hometalk.onepass.billing.scheduler;

import com.hometalk.onepass.billing.dto.BillingMonthlyStats;
import com.hometalk.onepass.billing.dto.UserOldestUnpaidProjection;
import com.hometalk.onepass.billing.repository.BillingRepository;
import com.hometalk.onepass.notification.entity.NotificationTargetRole;
import com.hometalk.onepass.notification.entity.NotificationType;
import com.hometalk.onepass.notification.publisher.NotificationPublisher;
import com.hometalk.onepass.notification.service.NotificationService;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 관리비 알림 스케줄러
 *
 * 실행 주기
 *   - 매일 08:55        → 미납/체납 자동 감지 + 입주민 알림 발송
 *   - 매월 1일 09:00    → 전월 정산 요약 관리자 전체 발송
 *
 * 미납/체납 판정 기준: 가장 오래된 미납 건의 dueDate 기준 경과 개월 수
 *   - 3개월 이상 → BILLING_OVERDUE (체납)
 *   - 1~2개월   → BILLING_UNPAID  (미납)
 *   - 당월(0개월) → skip
 *
 * 중복 발송 방지
 *   referenceId = billingId (가장 오래된 미납 건의 id)
 *   → 동일 (type, userId, referenceId) 조합이면
 *     NotificationService 내부에서 UPDATE만 수행 (중복 INSERT 없음)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillingScheduler {

    private final BillingRepository      billingRepository;
    private final NotificationPublisher  notificationPublisher;
    private final NotificationService    notificationService;

    // ─────────────────────────────────────────────
    // 매일 08:55 — 미납/체납 감지
    // ─────────────────────────────────────────────

    @Transactional
    @Scheduled(cron = "0 55 8 * * *")
    public void detectUnpaidAndNotify() {
        LocalDate today = LocalDate.now();
        log.info("[BillingScheduler] 미납/체납 감지 시작 - {}", today);

        List<UserOldestUnpaidProjection> unpaidList =
                billingRepository.findOldestUnpaidByUser(today);

        int unpaidCount  = 0;
        int overdueCount = 0;
        int skipCount    = 0;

        for (UserOldestUnpaidProjection proj : unpaidList) {
            Long      userId      = proj.getUserId();
            Long      billingId   = proj.getBillingId();
            LocalDate oldestDue   = proj.getOldestDueDate();

            // 경과 개월 수 (일 수 무시, 월 단위 계산)
            long monthsElapsed = ChronoUnit.MONTHS.between(
                    oldestDue.withDayOfMonth(1),
                    today.withDayOfMonth(1)
            );

            if (monthsElapsed >= 3) {
                // ── 체납 처리 ──────────────────────────────────────────
                // 미납 알림이 남아있으면 제거 (미납 → 체납 상태 변경 시)
                notificationService.deleteByTypeAndUser(
                        NotificationType.BILLING_UNPAID, userId);

                notificationPublisher.publish(
                        userId,
                        NotificationTargetRole.RESIDENT,
                        NotificationType.BILLING_OVERDUE,
                        "관리비 체납 안내",
                        monthsElapsed + "개월 이상 관리비가 미납 상태입니다. 관리사무소로 연락 부탁드립니다.",
                        "/billing",
                        billingId
                );
                overdueCount++;

            } else if (monthsElapsed >= 1) {
                // ── 미납 처리 ──────────────────────────────────────────
                // 체납 알림이 남아있으면 제거 (체납 → 미납 내려온 엣지케이스 대비)
                notificationService.deleteByTypeAndUser(
                        NotificationType.BILLING_OVERDUE, userId);

                notificationPublisher.publish(
                        userId,
                        NotificationTargetRole.RESIDENT,
                        NotificationType.BILLING_UNPAID,
                        "관리비 미납 안내",
                        monthsElapsed + "개월분 관리비가 아직 납부되지 않았습니다. 관리사무소로 연락 부탁드립니다.",
                        "/billing",
                        billingId
                );
                unpaidCount++;

            } else {
                // ── 당월(기한 내) — skip ───────────────────────────────
                skipCount++;
            }
        }

        if (overdueCount > 0) {
            notificationPublisher.publishToAll(
                    NotificationTargetRole.ADMIN,
                    NotificationType.BILLING_OVERDUE_ALERT,
                    "체납 세대 발생",
                    "체납 세대가 발생했습니다. 미납 관리 페이지를 확인해 주세요.",
                    "/billing/admin/unpaid"
            );
        }

        log.info("[BillingScheduler] 감지 완료 — 미납: {}건, 체납: {}건, 기한내 skip: {}건",
                unpaidCount, overdueCount, skipCount);
    }

    // ─────────────────────────────────────────────
    // 매월 1일 09:00 — 전월 정산 요약
    // ─────────────────────────────────────────────

    @Transactional
    @Scheduled(cron = "0 0 9 1 * *")
    public void sendMonthlySummary() {
        YearMonth lastMonth   = YearMonth.now().minusMonths(1);
        String    billingMonth = lastMonth.toString(); // "2026-04"

        log.info("[BillingScheduler] 전월 정산 요약 발송 시작 - {}", billingMonth);

        BillingMonthlyStats stats = billingRepository.getMonthlyStats(billingMonth);

        String title = lastMonth.getYear() + "년 "
                + lastMonth.getMonthValue() + "월 정산 요약";

        double paidRate = stats.getTotalCount() > 0
                ? (double) stats.getPaidCount() / stats.getTotalCount() * 100.0
                : 0.0;

        String message = String.format(
                "월초보고 [%d년 %d월 정산] 납부율 %.1f%% — 총 %d세대 중 납부 %d세대 / 미납 %d세대",
                lastMonth.getYear(),
                lastMonth.getMonthValue(),
                paidRate,
                stats.getTotalCount(),
                stats.getPaidCount(),
                stats.getUnpaidCount()
        );

        notificationPublisher.publishToAll(
                NotificationTargetRole.ADMIN,
                NotificationType.BILLING_MONTHLY_SUMMARY,
                title,
                message,
                "/billing/admin/unpaid"
        );

        log.info("[BillingScheduler] 정산 요약 발송 완료 - {}", message);
    }
}