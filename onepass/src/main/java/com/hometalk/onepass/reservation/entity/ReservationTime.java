package com.hometalk.onepass.reservation.entity;


// 예약 시작/종료 시간

import jakarta.persistence.Embeddable;
import lombok.*;

import java.time.Duration;
import java.time.LocalDateTime;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder

public class ReservationTime {
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /*
        총 예약 시간을 시간 단위로 계산
     */
    public long getDurationHours() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return Duration.between(startTime, endTime).toHours();
    }
    /*
        총 예약 시간을 분 단위로 계산
     */
    public long getDurationMinutes() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return Duration.between(startTime, endTime).toMinutes();
    }

    /*
        예약 시간 유효성 체크 (종료가 시작보다 빠른지)
     */
    public boolean isValid() {
        return startTime != null && endTime != null && endTime.isAfter(startTime);
    }

}
