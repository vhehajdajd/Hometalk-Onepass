package com.hometalk.onepass.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
* 입주민 대시보드
* */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResidentDashboardResponse {
    private String billingMonth;  // 예: "2월"
    private String status;        // "UNPAID" 또는 "PAID"
    private long totalAmount;     // 245,600
    private String dueDate;       // "2026년 3월 10일"
}