package com.hometalk.onepass.billing.repository;

import com.hometalk.onepass.billing.entity.BillingLog;
import com.hometalk.onepass.billing.entity.BillingActionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillingLogRepository extends JpaRepository<BillingLog, Long> {

    // 최근 납부일 조회 (입주민 요약 카드)
    Optional<BillingLog> findTopByBilling_Household_IdAndActionTypeOrderByCreatedAtDesc(
            Long householdId,
            BillingActionType actionType
    );

    // 납부일 조회 (고지서 모달 paidAt)
    Optional<BillingLog> findTopByBilling_IdAndActionTypeOrderByCreatedAtDesc(
            Long billingId,
            BillingActionType actionType
    );
}