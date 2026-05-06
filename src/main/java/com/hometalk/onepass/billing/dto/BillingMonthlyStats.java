package com.hometalk.onepass.billing.dto;

import java.math.BigDecimal;

/**
 * 전월 관리비 정산 통계 JPA Projection
 *
 * 사용처: BillingRepository.getMonthlyStats()
 *         → BillingScheduler.sendMonthlySummary()
 *         → BILLING_MONTHLY_SUMMARY 알림 메시지 내용 구성
 *
 * 필드 설명
 *   totalCount   — 해당 월 전체 세대 수
 *   paidCount    — 납부 완료 세대 수
 *   unpaidCount  — 미납 세대 수 (UNPAID)
 *   totalAmount  — 전체 고지 금액 합계
 *   paidAmount   — 납부 완료 금액 합계
 *   unpaidAmount — 미납 금액 합계
 */
public interface BillingMonthlyStats {

    int getTotalCount();

    int getPaidCount();

    int getUnpaidCount();

    BigDecimal getTotalAmount();

    BigDecimal getPaidAmount();

    BigDecimal getUnpaidAmount();
}