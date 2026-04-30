package com.hometalk.onepass.billing.service;

import com.hometalk.onepass.auth.entity.Household;
import com.hometalk.onepass.auth.repository.HouseholdRepository;
import com.hometalk.onepass.billing.entity.Billing;
import com.hometalk.onepass.billing.entity.BillingActionType;
import com.hometalk.onepass.billing.entity.BillingDetail;
import com.hometalk.onepass.billing.entity.BillingLog;
import com.hometalk.onepass.billing.entity.BillingStatus;
import com.hometalk.onepass.billing.repository.BillingDetailRepository;
import com.hometalk.onepass.billing.repository.BillingLogRepository;
import com.hometalk.onepass.billing.repository.BillingRepository;
import lombok.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BillingUploadService {

    private final BillingRepository       billingRepository;
    private final BillingDetailRepository billingDetailRepository;
    private final BillingLogRepository    billingLogRepository;
    private final HouseholdRepository     householdRepository;

    // ─────────────────────────────────────────────
    // 유효성 검사 + 미리보기
    // ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UploadPreviewResult validateAndPreview(List<UploadRow> rows) {

        List<UploadPreviewRow> previewRows = new ArrayList<>();
        int errorCount = 0;

        for (int i = 0; i < rows.size(); i++) {
            UploadRow row = rows.get(i);
            int num = i + 1;

            String validationError = validate(row);
            boolean hasError = validationError != null;
            if (hasError) errorCount++;

            Long billingId = null;
            String dong = null;
            String ho = null;
            UpsertType upsertType = UpsertType.ERROR;

            if (!hasError) {
                Optional<Household> householdOpt = findHousehold(row.getHouseholdId());

                if (householdOpt.isPresent()) {
                    Household household = householdOpt.get();
                    dong = household.getDong();
                    ho = household.getHo();

                    Optional<Billing> existing = billingRepository
                            .findByHousehold_IdAndBillingMonth(
                                    household.getId(), row.getBillingMonth());

                    if (existing.isPresent()) {
                        billingId = existing.get().getId();
                        upsertType = UpsertType.UPDATE;
                    } else {
                        upsertType = UpsertType.INSERT;
                    }
                } else {
                    upsertType = UpsertType.INSERT;
                }
            }

            previewRows.add(UploadPreviewRow.builder()
                    .num(num)
                    .householdId(row.getHouseholdId())
                    .dong(dong)
                    .ho(ho)
                    .billingId(billingId)
                    .billingMonth(row.getBillingMonth())
                    .totalAmount(row.getTotalAmount())
                    .validationError(validationError)
                    .upsertType(upsertType)
                    .build());
        }

        return new UploadPreviewResult(rows.size(), errorCount, previewRows);
    }

    // ─────────────────────────────────────────────
    // 업로드 확정 (UPSERT)
    // ─────────────────────────────────────────────

    @Transactional
    public UploadConfirmResult confirmUpload(List<UploadRow> rows, Long adminId) {

        int insertCount = 0;
        int updateCount = 0;

        for (UploadRow row : rows) {
            if (validate(row) != null) continue;

            Optional<Household> householdOpt = findHousehold(row.getHouseholdId());
            if (householdOpt.isEmpty()) continue;

            Household household = householdOpt.get();

            Optional<Billing> existing = billingRepository
                    .findByHousehold_IdAndBillingMonth(
                            household.getId(), row.getBillingMonth());

            Billing billing;

            if (existing.isPresent()) {
                billing = existing.get();
                billing.updateByUpload(row.getTotalAmount(), row.getDueDate());
                billingDetailRepository.deleteByBilling_Id(billing.getId());
                updateCount++;
            } else {
                billing = billingRepository.save(Billing.builder()
                        .household(household)
                        .billingMonth(row.getBillingMonth())
                        .dueDate(row.getDueDate())
                        .totalAmount(row.getTotalAmount())
                        .status(BillingStatus.UNPAID)
                        .lastUploadType("INSERT")
                        .build());
                insertCount++;
            }

            List<BillingDetail> details = new ArrayList<>();
            List<ItemRow> items = row.getItems();
            for (int i = 0; i < items.size(); i++) {
                details.add(BillingDetail.builder()
                        .billing(billing)
                        .itemName(items.get(i).getItemName())
                        .itemAmount(items.get(i).getItemAmount())
                        .sortOrder(i)
                        .build());
            }
            billingDetailRepository.saveAll(details);
        }

        if (insertCount + updateCount > 0) {
            billingLogRepository.save(BillingLog.builder()
                    .billing(null)
                    .userId(adminId)
                    .actionType(BillingActionType.UPLOAD)
                    .build());
        }

        return new UploadConfirmResult(insertCount, updateCount);
    }

    // ─────────────────────────────────────────────
    // 유효성 검사
    // ─────────────────────────────────────────────

    private String validate(UploadRow row) {
        if (row.getHouseholdId() == null || row.getHouseholdId().isBlank()) return "세대 정보 누락";
        if (row.getBillingMonth() == null || row.getBillingMonth().isBlank()) return "부과월 누락";
        if (row.getTotalAmount() == null || row.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) return "금액 누락"; // 0원도 누락으로 처리
        if (row.getDueDate() == null) return "납기일 누락";
        if (row.getItems() == null || row.getItems().isEmpty()) return "상세 항목 누락";
        return null;
    }

    // ─────────────────────────────────────────────
    // 내부 유틸
    // ─────────────────────────────────────────────

    private Optional<Household> findHousehold(
            String householdId, String postNum) {
        String[] parts = householdId.split("-");
        if (parts.length < 2) return Optional.empty();
        String dong = parts[0] + "동";
        String ho   = parts[1] + "호";
        return householdRepository
                .findByPostNumAndDongAndHo(postNum, dong, ho);
    }

    // ─────────────────────────────────────────────
    // 데이터 클래스
    // ─────────────────────────────────────────────

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadRow {
        private String        householdId;
        private String        billingMonth;
        private LocalDate     dueDate;
        private BigDecimal    totalAmount;
        private List<ItemRow> items;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemRow {
        private String     itemName;
        private BigDecimal itemAmount;
    }

    @Getter
    @Builder
    public static class UploadPreviewRow {
        private int        num;
        private String     householdId;
        private String     billingMonth;
        private BigDecimal totalAmount;
        private String     validationError;
        private UpsertType upsertType;
        private Long       billingId;
        private String     dong;
        private String     ho;
    }

    public record UploadPreviewResult(
            int totalCount,
            int errorCount,
            List<UploadPreviewRow> rows
    ) {}

    public record UploadConfirmResult(
            int insertCount,
            int updateCount
    ) {}

    public enum UpsertType {
        INSERT, UPDATE, ERROR
    }
}