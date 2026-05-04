package com.hometalk.onepass.notification.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {

    // ─── 관리비 — 입주민 ───
    BILLING_UPLOAD ("관리비", "🏠", 7, 24),  // 전체 입주민, 7일, 24h dedupe
    BILLING_PAID   ("관리비", "🏠", 1,  0),  // 해당 입주민, 24시간 후 자동삭제
    BILLING_UNPAID ("관리비", "⚠️", 0,  0), // 영구 (조건부 삭제)
    BILLING_OVERDUE("관리비", "🚨", 0,  0), // 영구 (조건부 삭제)

    // ─── 관리비 — 관리자 ───
    BILLING_UPLOAD_DONE     ("관리비", "✅", 7,  1),
    BILLING_UPLOAD_ERROR    ("관리비", "❌", 7,  1),
    BILLING_OVERDUE_ALERT   ("관리비", "🚨", 7, 24),
    BILLING_MONTHLY_SUMMARY ("관리비", "📊", 7, 24),

    // ─── 공지 ───
    NOTICE_NEW ("공지", "📢", 7, 1),

    // ─── 커뮤니티 ───
    COMMUNITY_COMMENT ("커뮤니티", "💬", 7, 0),

    // ─── 민원 ───
    COMPLAINT_RECEIVED ("민원", "📋", 7, 0),
    COMPLAINT_STATUS   ("민원", "📋", 7, 0),

    // ─── 주차 ───
    VEHICLE_APPROVED ("주차", "🚗", 3, 0),
    VEHICLE_REJECTED ("주차", "🚗", 3, 0),
    VEHICLE_ENTRY    ("주차", "🚗", 1, 0),
    VEHICLE_EXIT     ("주차", "🚗", 1, 0),

    // ─── 시설예약 ───
    RESERVATION_CONFIRMED ("예약", "📅", 3, 0);

    private final String category;
    private final String icon;
    private final int    expireDays;        // 0 = 영구(조건부 삭제) / N = N일 후 자동삭제
    private final int    dedupeWindowHours; // 중복 방지 시간 윈도우 (0 = 미적용)
}