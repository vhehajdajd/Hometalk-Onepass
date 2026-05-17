package com.hometalk.onepass.reservation.entity;

public enum ReservationStatus {

    PENDING("승인 대기"),        // 승인 대기
    CONFIRMED("예약 확정"),      // 예약 확정
    CANCELED("취소"),           // 예약 취소
    REJECTED("반려"),           // 관리자 예약 불허
    COMPLETED("방문 완료"),      // 입실 확인 완료 상태: QR/NFC/관리자 체크인 기능 확장 시 사용
    FINISHED("이용 종료");       // 실제 시설 이용 종료 상태: 사용자가 종료 처리하거나 종료 시간이 지난 상태

    private final String title;

    ReservationStatus(String title) {
        this.title = title;
    }
    public String getTitle() {
        return title;
    }
}
