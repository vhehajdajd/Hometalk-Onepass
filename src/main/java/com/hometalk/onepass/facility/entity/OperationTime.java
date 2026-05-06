package com.hometalk.onepass.facility.entity;

import jakarta.persistence.Embeddable;
import lombok.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor // Builder를 위해 필수
@Builder
public class OperationTime {

    private LocalTime openTime;
    private LocalTime closeTime;

    /**
     * 화면 출력용 편의 메서드 (HH:mm 형식)
     * 예: 09:00, 22:00
     */
    public String getFormattedOpenTime() {
        return openTime != null ? openTime.format(DateTimeFormatter.ofPattern("HH:mm")) : "";
    }

    public String getFormattedCloseTime() {
        return closeTime != null ? closeTime.format(DateTimeFormatter.ofPattern("HH:mm")) : "";
    }
}