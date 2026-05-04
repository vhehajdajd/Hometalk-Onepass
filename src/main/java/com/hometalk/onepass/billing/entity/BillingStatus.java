package com.hometalk.onepass.billing.entity;

/*
    Enum 분리 (UNPAID / PAID)
 */

/*
 * 관리비 납부 상태 Enum
 *
 * 사용처:
 *  - Billings.status 컬럼 (DB ENUM('UNPAID','PAID'))
 *  - BillingDetailResponse.status
 *  - BillingSummaryResponse.status
 *  - 입주민 화면: 배지 색상 분기 (UNPAID 주황 / PAID 초록)
 *  - 관리자 미납 세대 목록: 납부완료 처리 버튼 활성화 기준
 */
public enum BillingStatus {

    /* 미납 — 엑셀 업로드 시 기본값 */
    UNPAID,

    /* 납부 완료 — 관리자 납부완료 처리 confirm 시 변경 */
    PAID
}