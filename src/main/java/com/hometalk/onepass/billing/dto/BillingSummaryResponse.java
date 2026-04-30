package com.hometalk.onepass.billing.dto;

/*
 * 관리비 목록 행 1건 응답 DTO
 *
 * 사용처:
 *  - 입주민 관리비 내역 리스트
 *  - 관리자 미납 세대 관리 목록
 *  - 관리자 고지서 업로드 유효성 검사 + 미리보기 테이블
 */

import com.hometalk.onepass.billing.entity.Billing;
import com.hometalk.onepass.billing.entity.BillingStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class BillingSummaryResponse {

    private final Long          billingId;       // Billing PK
    private final Long          householdId;     // Household FK (숫자)
    private final String        dong;            // "103동"
    private final String        ho;              // "1101호"
    private final String        unit;            // "103동 1101호" (화면 표시용)
    private final String        residentName;    // 입주민 이름
    private final String        billingMonth;    // "2026-01"
    private final LocalDate     dueDate;
    private final BigDecimal    totalAmount;
    private final BillingStatus status;
    private final String        upsertType;      // "INSERT" | "UPDATE" (미리보기 전용)
    private final String        lastUploadType;  // "INSERT" / "UPDATE" / null

    // 입주민/업로드 DB 조회용
    public static BillingSummaryResponse from(Billing billing) {
        return BillingSummaryResponse.builder()
                .billingId(billing.getId())
                .householdId(billing.getHousehold().getId())
                .dong(billing.getHousehold().getDong())
                .ho(billing.getHousehold().getHo())
                .unit(billing.getHousehold().getDong() + " " + billing.getHousehold().getHo())
                .billingMonth(billing.getBillingMonth())
                .dueDate(billing.getDueDate())
                .totalAmount(billing.getTotalAmount())
                .status(billing.getStatus())
                .lastUploadType(billing.getLastUploadType())
                .build();

    }

    // 관리자 미납 목록용 (입주민명 포함)
    public static BillingSummaryResponse of(Billing billing, String residentName) {
        return BillingSummaryResponse.builder()
                .billingId(billing.getId())
                .householdId(billing.getHousehold().getId())
                .dong(billing.getHousehold().getDong())
                .ho(billing.getHousehold().getHo())
                .unit(billing.getHousehold().getDong() + " " + billing.getHousehold().getHo())
                .residentName(residentName)
                .billingMonth(billing.getBillingMonth())
                .dueDate(billing.getDueDate())
                .totalAmount(billing.getTotalAmount())
                .status(billing.getStatus())
                .lastUploadType(billing.getLastUploadType())
                .build();
    }

}