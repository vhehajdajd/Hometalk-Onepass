package com.hometalk.onepass.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/*
* 입주민 대시보드
* */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResidentDashboardResponse {
    private String billingMonth;
    private String status;
    private BigDecimal totalAmount;
    private String dueDate;
    private boolean overdue;  // true면 "체납 3개월 - 관리사무소 문의요망" 표시
}
