package com.hometalk.onepass.facility.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BookingStatus {
    AVAILABLE("예약가능"),
    ALREADY_BOOKED("예약중"),
    CLOSED("운영종료");

    private final String description;
}
