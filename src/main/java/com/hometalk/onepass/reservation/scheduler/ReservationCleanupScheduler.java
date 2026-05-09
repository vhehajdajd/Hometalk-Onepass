package com.hometalk.onepass.reservation.scheduler;

import com.hometalk.onepass.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationCleanupScheduler {

    private final ReservationRepository reservationRepository;

    @Transactional
    @Scheduled(cron = "0 0 3 * * *")
    public void deleteOldReservations() {
        LocalDateTime cutoffDateTime = LocalDateTime.now().minusDays(7);

        reservationRepository.deleteByReservationTime_EndTimeBefore(cutoffDateTime);

        log.info("7일 지난 예약 내역 자동 삭제 완료. 기준 시간: {}", cutoffDateTime);
    }
}