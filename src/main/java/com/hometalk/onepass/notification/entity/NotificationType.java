package com.hometalk.onepass.notification.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {

    // ─── 관리비 — 입주민 ───
    BILLING_UPLOAD ("관리비", "🏠", 7, 24),  // 전체 입주민, 이번 달 관리비 고지서 나왔음
    BILLING_PAID   ("관리비", "🏠", 1,  0),  // 해당 입주민, 관리비 납부 처리되었음. 24시간 후 자동삭제
    BILLING_UNPAID ("관리비", "⚠️", 0,  0), // 해당 입주민, 관리비 미납. 영구 (조건부 삭제)
    BILLING_OVERDUE("관리비", "🚨", 0,  0), // 해당 입주민, 관리비 3개월 이상 체납. 영구 (조건부 삭제)

    // ─── 관리비 — 관리자 ───
    BILLING_UPLOAD_DONE     ("관리비", "✅", 7,  1), // 전체관리자, 고지서 업로드 정상적으로 완료
    BILLING_OVERDUE_ALERT   ("관리비", "🚨", 7, 24), // 전체관리자, 체납 세대 발생
    BILLING_MONTHLY_SUMMARY ("관리비", "📊", 7, 24), // 전체관리자, 월초. 전월 관리비 납부 현황 요약

    // ─── 공지 ───
    NOTICE_NEW ("공지", "📢", 7, 1),

    // ─── 커뮤니티 ───
    COMMUNITY_COMMENT ("커뮤니티", "💬", 1, 0), // 해당 입주민, 내 게시글에 새 댓글. 24시간 후 자동삭제

    // ─── 민원 ───
    COMPLAINT_RECEIVED ("민원", "📋", 7, 0),
    COMPLAINT_STATUS   ("민원", "📋", 7, 0),

    // ─── 주차 ───
    VEHICLE_APPROVED        ("주차", "🚗", 7, 0), // 해당 입주민, 차량 등록 승인
    VEHICLE_REJECTED        ("주차", "🚗", 7, 0), // 해당 입주민, 차량 등록 반려
    VEHICLE_ENTRY           ("주차", "🚗", 1, 0), // 해당 입주민, 입주자 입차
    VEHICLE_VISITOR_ENTRY   ("주차", "🚗", 1, 0), // 해당 입주민, 예약 방문객 입차
    VEHICLE_EXIT            ("주차", "🚗", 1, 0), // 해당 입주민, 입주자 출차
    VEHICLE_VISITOR_EXIT    ("주차", "🚗", 1, 0), // 해당 입주민, 예약 방문객 출차
    VEHICLE_TICKET_SHORTAGE ("주차", "🚗", 1, 0), // 해당 입주민, 티켓 부족 출차 불가

    // ─── 시설예약 ───
    RESERVATION_CONFIRMED ("예약", "📅", 3, 0);

    private final String category;
    private final String icon;
    private final int    expireDays;        // 0 = 영구(조건부 삭제) / N = N일 후 자동삭제
    private final int    dedupeWindowHours; // 중복 방지 시간 윈도우 (0 = 미적용)
}