package com.hometalk.onepass.reservation.entity;

public enum ReservationStatus {

    PENDING("승인 대기"),        // 승인 대기
    CONFIRMED("예약 확정"),      // 예약 확정
    CANCELED("취소됨"),          // 예약 취소
    COMPLETED("방문 완료"),      // 예약 완료  -- 확장 이후 입실 확인 시 '방문 완료'로 변경
    FINISHED("이용 종료");       // 이용 종료

    private final String title;

    ReservationStatus(String title) {
        this.title = title;
    }
    public String getTitle() {
        return title;
    }
}
