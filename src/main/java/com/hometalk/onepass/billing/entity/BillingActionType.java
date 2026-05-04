package com.hometalk.onepass.billing.entity;
/*
    Enum 분리 (UPLOAD / STATUS_CHANGE)
 */
/*
 * 관리비 관리자 행위 유형 Enum
 *
 * 사용처:
 *  - BillingLogs.action_type 컬럼
 *  - 알림(NOTIFICATIONS) 트리거 분기 기준
 *
 * 알림 연동:
 *  - UPLOAD        : module_name=BILLING, category=NEW 알림 생성
 *  - STATUS_CHANGE : 입주민 납부 완료 알림 트리거
 */
public enum BillingActionType {

    /* 관리자 엑셀 업로드 확정 */
    UPLOAD,

    /* 납부 상태 변경 (UNPAID → PAID) */
    STATUS_CHANGE
}