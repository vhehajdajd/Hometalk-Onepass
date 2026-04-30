package com.hometalk.onepass.notice.entity;

public enum Badge {
    FACILITY("시설"),
    SAFETY("안전"),
    NORMAL("일반");

    private final String label;

    Badge(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}