package com.hometalk.onepass.dashboard.dto.notification.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NotificationToHouseholdMatchRequest {

    private Long reservationId;

    private Long parkingId;

    // reservationId 우선, 없으면 parkingId 사용
    public Long getEffectiveId() {
        return reservationId != null ? reservationId : parkingId;
    }
}
