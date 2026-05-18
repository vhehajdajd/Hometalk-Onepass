package com.hometalk.onepass.community.enums;

import lombok.Getter;

@Getter
public enum ReportReason {
    SPAM("광고 및 도배"),
    ABUSE("욕설 및 비방"),
    PROFANITY("음란성 내용"),
    INAPPROPRIATE("주민 정서 부적절"),
    ETC("기타 사유");

    private final String description;

    ReportReason(String description) {
        this.description = description;
    }
}
