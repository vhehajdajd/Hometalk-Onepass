package com.hometalk.onepass.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
* 관리자 대시보드
* */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {
    private String billingMonth;        // 예: "2월"
    private long unpaidHouseholds;      // 미납 세대 수 (12세대)
    private long totalUnpaidAmount;     // 미납 총액 (3,450,000원)
}