package com.hometalk.onepass.billing.dto;

/*
 * 입주민 관리비 페이지 전체 응답 DTO
 *
 * 사용처:
 *  - 입주민 관리비 페이지 최초 진입 시 한 번에 응답
 *    · API: GET /hometop/api/billing (household_id는 Spring Security에서 추출)
 *
 * 화면 구성:
 *  - 상단 미납 경고 배너: hasUnpaid + latestUnpaidMonth 기준 노출
 *  - 요약 카드 3개: currentMonthAmount / unpaidCount / lastPaidDate
 *  - 관리비 내역 리스트: billings (BillingSummaryResponse 배열)
 */
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class ResidentBillingResponse {

    // 미납 배너
    private final boolean hasUnpaid;
    private final String latestUnpaidMonth;     // "2026년 2월"

    // 요약 카드
    private final BigDecimal currentMonthAmount;
    private final int unpaidCount;
    private final LocalDate lastPaidDate;

    // 내역 리스트
    private final List<BillingSummaryResponse> billings;
}