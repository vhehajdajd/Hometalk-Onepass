package com.hometalk.onepass.complaint.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ComplaintStatus {
    WAITING("접수완료", "status-waiting"),
    CHECK("확인", "status-check"),
    PROCESSING("처리중", "status-processing"),
    COMPLETED("처리완료", "status-finished");

    private final String description;
    private final String cssClass;
}