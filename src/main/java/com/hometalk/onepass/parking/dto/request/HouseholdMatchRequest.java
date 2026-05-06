package com.hometalk.onepass.parking.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class HouseholdMatchRequest {

    private Long reservationId;

    private Long parkingId;

    // reservationId 우선, 없으면 parkingId 사용
    public Long getEffectiveId() {
        return reservationId != null ? reservationId : parkingId;
    }
}