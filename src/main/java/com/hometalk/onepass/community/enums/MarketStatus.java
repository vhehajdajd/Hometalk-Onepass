package com.hometalk.onepass.community.enums;

import lombok.Getter;

@Getter
public enum MarketStatus {
    SHARED("나눔중"),
    RESERVED("예약중"),
    SOLD("완료");

    private final String description;

    MarketStatus(String description) {
        this.description = description;
    }
}
