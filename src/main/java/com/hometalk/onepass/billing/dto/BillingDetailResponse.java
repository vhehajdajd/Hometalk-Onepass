package com.hometalk.onepass.billing.dto;

import com.hometalk.onepass.billing.entity.Billing;
import com.hometalk.onepass.billing.entity.BillingDetail;
import com.hometalk.onepass.billing.entity.BillingStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 고지서 상세 응답 DTO
 * <p>
 * 사용처:
 *   - 관리자 고지서 업로드 미리보기 모달 (mode='preview')
 *   - 입주민 디지털 고지서 모달        (mode='resident')
 * <p>
 * API: GET /api/billing/{billingId}/detail
 */
@Getter
@Builder
public class BillingDetailResponse {

    private final Long          billingId;
    private final String        billingMonth;   // "2026-03"
    private final String        dongHo;         // "101동 1204호"
    private final LocalDate     dueDate;
    private final BigDecimal    totalAmount;
    private final BillingStatus status;
    private final List<ItemDetail> items;

    public static BillingDetailResponse from(Billing billing, List<BillingDetail> details) {
        return BillingDetailResponse.builder()
                .billingId(billing.getId())
                .billingMonth(billing.getBillingMonth())
                .dongHo(billing.getHousehold().getDong() + " " + billing.getHousehold().getHo())
                .dueDate(billing.getDueDate())
                .totalAmount(billing.getTotalAmount())
                .status(billing.getStatus())
                .items(details.stream()
                        .map(d -> ItemDetail.builder()
                                .itemName(d.getItemName())
                                .itemAmount(d.getItemAmount())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    @Getter
    @Builder
    public static class ItemDetail {
        private final String     itemName;
        private final BigDecimal itemAmount;
    }
}