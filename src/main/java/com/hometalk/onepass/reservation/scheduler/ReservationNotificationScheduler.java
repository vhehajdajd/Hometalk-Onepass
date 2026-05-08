package com.hometalk.onepass.reservation.scheduler;

import com.hometalk.onepass.notification.entity.NotificationTargetRole;
import com.hometalk.onepass.notification.entity.NotificationType;
import com.hometalk.onepass.notification.publisher.NotificationPublisher;
import com.hometalk.onepass.reservation.entity.Reservation;
import com.hometalk.onepass.reservation.repository.ReservationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 시설 예약 알림 스케줄러
 *
 * 실행 주기
 *   - 매분 실행 — 1시간 후 시작하는 확정 예약 감지 → 예약자에게 알림 발송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationNotificationScheduler {

    private final ReservationRepository reservationRepository;
    private final NotificationPublisher notificationPublisher;

    /**
     * 매분 실행 — 정확히 1시간(±1분) 후 시작하는 확정 예약 조회 후 알림 발송
     */
    @Transactional
    @Scheduled(cron = "0 * * * * *")
    public void notifyUpcomingReservations() {

        LocalDateTime from = LocalDateTime.now().plusHours(1);
        LocalDateTime to = from.plusMinutes(1);

        List<Reservation> reservations =
                reservationRepository.findConfirmedReservationsStartingBetween(from, to);

        if (reservations.isEmpty()) {
            return;
        }

        for (Reservation reservation : reservations) {

            String facilityName = reservation.getFacility().getName();
            LocalDateTime startTime = reservation.getReservationTime().getStartTime();

            String message = String.format(
                    "%s %d시 %d분 예약 한시간 전입니다.",
                    facilityName,
                    startTime.getHour(),
                    startTime.getMinute()
            );



            log.info(
                    "[ReservationNotificationScheduler] 예약 알림 발송 - reservationId: {}, userId: {}",
                    reservation.getId(),
                    reservation.getUser().getId()
            );
        }
    }
}