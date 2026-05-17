package com.hometalk.onepass.community.enums;

import lombok.Getter;

@Getter
public enum TradeType {

    SELL("판매"),
    BUY("구매");
    private final String description;

    TradeType(String description) {
        this.description = description;
    }
}
