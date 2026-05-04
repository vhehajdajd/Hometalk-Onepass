package com.hometalk.onepass.billing.repository;

import com.hometalk.onepass.billing.entity.BillingDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillingDetailRepository extends JpaRepository<BillingDetail, Long> {

    List<BillingDetail> findByBilling_IdOrderBySortOrderAsc(Long billingId);

    void deleteByBilling_Id(Long billingId);
}