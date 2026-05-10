package com.hometalk.onepass.schedule.scheduler;

import com.hometalk.onepass.notification.entity.NotificationTargetRole;
import com.hometalk.onepass.notification.entity.NotificationType;
import com.hometalk.onepass.notification.publisher.NotificationPublisher;
import com.hometalk.onepass.schedule.dto.ScheduleCalResponseDto;
import com.hometalk.onepass.schedule.service.ScheduleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 일정 알림 스케줄러
 *
 * 실행 주기
 *   - 매일 10:00 — 내일 일정 있는 경우 전체 입주민 + 관리자 발송
 *   - 매일 08:00 — 오늘 일정 있는 경우 전체 입주민 + 관리자 발송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleNotificationScheduler {

    private final ScheduleService       scheduleService;
    private final NotificationPublisher notificationPublisher;

    // ─────────────────────────────────────────────
    // 매일 10:00 — 내일 일정 알림
    // ─────────────────────────────────────────────
    @Transactional
    @Scheduled(cron = "0 0 10 * * *")
    public void notifyTomorrowSchedules() {
        List<ScheduleCalResponseDto> schedules = scheduleService.getTomorrowSchedules();

        if (schedules.isEmpty()) {
            log.info("[ScheduleNotificationScheduler] 내일 일정 없음 - skip");
            return;
        }

        for (ScheduleCalResponseDto schedule : schedules) {
            String message = "내일 " + schedule.getTitle() + " 일정이 있습니다.";

            // 전체 입주민 발송
            notificationPublisher.publishToAll(
                    NotificationTargetRole.RESIDENT,
                    NotificationType.SCHEDULE_TOMORROW,
                    schedule.getTitle(),
                    message,
                    "/schedule"
            );

            // 전체 관리자 발송
            notificationPublisher.publishToAll(
                    NotificationTargetRole.ADMIN,
                    NotificationType.SCHEDULE_TOMORROW,
                    schedule.getTitle(),
                    message,
                    "/schedule"
            );
        }

        log.info("[ScheduleNotificationScheduler] 내일 일정 알림 발송 완료 - {}건", schedules.size());
    }

    // ─────────────────────────────────────────────
    // 매일 08:00 — 오늘 일정 알림
    // ─────────────────────────────────────────────
    @Transactional
    @Scheduled(cron = "0 0 8 * * *")
    public void notifyTodaySchedules() {
        List<ScheduleCalResponseDto> schedules = scheduleService.getTodaySchedules();

        if (schedules.isEmpty()) {
            log.info("[ScheduleNotificationScheduler] 오늘 일정 없음 - skip");
            return;
        }

        for (ScheduleCalResponseDto schedule : schedules) {
            String message = "오늘 " + schedule.getTitle() + " 일정이 있습니다.";

            // 전체 입주민 발송
            notificationPublisher.publishToAll(
                    NotificationTargetRole.RESIDENT,
                    NotificationType.SCHEDULE_TODAY,
                    schedule.getTitle(),
                    message,
                    "/schedule"
            );

            // 전체 관리자 발송
            notificationPublisher.publishToAll(
                    NotificationTargetRole.ADMIN,
                    NotificationType.SCHEDULE_TODAY,
                    schedule.getTitle(),
                    message,
                    "/schedule"
            );
        }

        log.info("[ScheduleNotificationScheduler] 오늘 일정 알림 발송 완료 - {}건", schedules.size());
    }
}