package com.hometalk.onepass.reservation.scheduler;

import com.hometalk.onepass.reservation.entity.Reservation;
import com.hometalk.onepass.reservation.entity.ReservationStatus;
import com.hometalk.onepass.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationCleanupScheduler {

    private final ReservationRepository reservationRepository;

    @Transactional
    @Scheduled(cron = "0 0 3 * * *")
    public void deleteOldReservations() {
        LocalDateTime cutoffDateTime = LocalDateTime.now().minusDays(7);

        reservationRepository.bulkDeleteByStatusAndEndTime(
                ReservationStatus.CANCELED,
                cutoffDateTime
        );
        log.info("7일 경과된 '취소된 예약' 내역 자동 삭제 완료. 기준 시간: {}", cutoffDateTime);
    }

    /*
     * 매 분 0초마다 실행
     * 종료 시간이 경과한 'CONFIRMED' 예약을 'FINISHED' 상태로 자동 전환
     */
    @Transactional
    @Scheduled(cron = "0 * * * * *")
    public void autoFinishExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();

        List<Reservation> expired = reservationRepository
                .findByStatusAndReservationTime_EndTimeBefore(ReservationStatus.CONFIRMED, now);

        if (!expired.isEmpty()) {
            for (Reservation res : expired) {
                try {
                    res.finish();
                    log.info("[StatusScheduler] 자동 이용 종료 완료 - 예약ID: {}, 시설: {}",
                            res.getId(), res.getFacility().getName());
                } catch (Exception e) {
                    log.error("[StatusScheduler] 자동 종료 처리 중 에러 - ID: {}", res.getId(), e);
                }
            }
        }
    }
}