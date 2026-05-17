package com.hometalk.onepass.community.enums;

import lombok.Getter;

@Getter
public enum TradeStatus {

    SELLING("거래중"),
    RESERVED("예약중"),
    COMPLETED("완료");

    private final String description;

    TradeStatus(String description) {
        this.description = description;
    }
}

