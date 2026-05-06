package com.hometalk.onepass.community.enums;

import lombok.Getter;

@Getter
public enum MarketStatus {
    NONE(""),
    SHARED("나눔중"),
    RESERVED("예약중"),
    COMPLETED("나눔완료");

    private final String description;

    MarketStatus(String description) {
        this.description = description;
    }
}
