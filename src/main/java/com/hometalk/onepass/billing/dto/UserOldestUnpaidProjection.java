package com.hometalk.onepass.billing.dto;

import java.time.LocalDate;

/**
 * 유저별 가장 오래된 미납 billing 조회용 JPA Projection
 *
 * 사용처: BillingRepository.findOldestUnpaidByUser()
 *         → BillingScheduler.detectUnpaidAndNotify()
 *
 * 필드 설명
 *   userId        — 입주민 user.id (알림 발송 대상)
 *   billingId     — BillingScheduler에서 referenceId로 사용
 *   oldestDueDate — 미납/체납 판정 기준 날짜
 */
public interface UserOldestUnpaidProjection {

    Long getUserId();

    Long getBillingId();

    LocalDate getOldestDueDate();
}