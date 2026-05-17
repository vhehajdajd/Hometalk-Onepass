package com.hometalk.onepass.reservation.entity;

public enum ReservationPolicyType {
    INDIVIDUAL("개인 기준"),
    HOUSEHOLD("세대 기준"),
    GROUP("그룹 기준");

    private final String title;

    ReservationPolicyType(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
