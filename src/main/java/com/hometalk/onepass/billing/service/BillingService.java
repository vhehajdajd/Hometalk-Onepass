package com.hometalk.onepass.billing.service;

import com.hometalk.onepass.billing.dto.BillingDetailResponse;
import com.hometalk.onepass.billing.dto.BillingSummaryResponse;
import com.hometalk.onepass.billing.dto.ResidentBillingResponse;
import com.hometalk.onepass.billing.entity.Billing;
import com.hometalk.onepass.billing.entity.BillingActionType;
import com.hometalk.onepass.billing.entity.BillingDetail;
import com.hometalk.onepass.billing.entity.BillingLog;
import com.hometalk.onepass.billing.entity.BillingStatus;
import com.hometalk.onepass.billing.repository.BillingDetailRepository;
import com.hometalk.onepass.billing.repository.BillingLogRepository;
import com.hometalk.onepass.billing.repository.BillingRepository;
import com.hometalk.onepass.auth.repository.HouseholdRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final BillingRepository       billingRepository;
    private final BillingDetailRepository billingDetailRepository;
    private final BillingLogRepository    billingLogRepository;
    private final HouseholdRepository     householdRepository;

    // ─────────────────────────────────────────────
    // AdminBillingStats
    //   BillingPageController에서
    //   import com.hometalk.onepass.billing.service.BillingService.AdminBillingStats;
    //   로 사용 → 필드명 total / paid / unpaid / paidRate 고정
    // ─────────────────────────────────────────────

    public record AdminBillingStats(
            long   total,
            long   paid,
            long   unpaid,
            double paidRate
    ) {}

    // ─────────────────────────────────────────────
    // 관리자: 통계 (미납 세대 관리 상단 Summary Cards)
    // ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AdminBillingStats getAdminStats(String billingMonth) {
        long total  = billingRepository.countDistinctHouseholdByBillingMonth(billingMonth);
        long paid   = billingRepository.countByBillingMonthAndStatus(billingMonth, BillingStatus.PAID);
        long unpaid = billingRepository.countByBillingMonthAndStatus(billingMonth, BillingStatus.UNPAID);
        double rate = total > 0 ? Math.round((double) paid / total * 1000.0) / 10.0 : 0.0;
        return new AdminBillingStats(total, paid, unpaid, rate);
    }

    // 미납 관리 통계
    public record AdminDashboardStats(
            // 윗줄: 필터 기준
            long   totalHouseholds,    // household 테이블 전체 수 (항상 고정)
            long   paidCount,          // 필터 기준 PAID 수
            long   unpaidCount,        // 필터 기준 UNPAID 수
            double paidRate,           // paidCount / totalHouseholds * 100

            // 아랫줄: 전체 기간 고정
            long   globalUnpaidBillings,    // 전체 기간 UNPAID billing 건수
            long   globalUnpaidHouseholds,  // 전체 기간 미납 세대 수
            long   globalOverdueHouseholds  // 3개월 이상 체납 세대 수
    ) {}


    @Transactional(readOnly = true)
    public AdminDashboardStats getDashboardStats(
            Integer year, String month, String dong
    ) {
        // 전체 세대 수 (household 테이블 기준)
        long totalHouseholds = householdRepository.count();

        // 필터 파라미터 계산
        String yearFrom = (year != null && month == null) ? year + "-01" : null;
        String yearTo   = (year != null && month == null) ? year + "-12" : null;
        String monthParam = month; // "2026-01" 형식 또는 null

        // 윗줄: 필터 기준 통계
        long paidCount   = billingRepository.countPaidWithFilter(dong, yearFrom, yearTo, monthParam);
        long unpaidCount = billingRepository.countUnpaidWithFilter(dong, yearFrom, yearTo, monthParam);
        double paidRate  = totalHouseholds > 0
                ? Math.round((double) paidCount / totalHouseholds * 1000.0) / 10.0
                : 0.0;

        // 아랫줄: 전체 기간 고정 통계
        String overdueBefore = YearMonth.now().minusMonths(3).toString();
        long globalUnpaidBillings   = billingRepository.countAllUnpaid();
        long globalUnpaidHouseholds = billingRepository.countDistinctUnpaidHouseholds();
        long globalOverdueHouseholds = billingRepository.countDistinctOverdueHouseholds(overdueBefore);

        return new AdminDashboardStats(
                totalHouseholds, paidCount, unpaidCount, paidRate,
                globalUnpaidBillings, globalUnpaidHouseholds, globalOverdueHouseholds
        );
    }


    // ─────────────────────────────────────────────
    // 관리자: 고지서 전체 목록 (업로드 화면 DB 모드)
    //   BillingApiController.getAdminList() 에서 사용
    // ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<BillingSummaryResponse> getAdminBillingList(
            Integer year, String month, String dong, int size, int page
    ) {
        String yearFrom = (year != null && month == null) ? year + "-01" : null;
        String yearTo   = (year != null && month == null) ? year + "-12" : null;

        PageRequest pageable = PageRequest.of(page, size,
                Sort.by("billingMonth").descending()
                        .and(Sort.by("household.dong").ascending())
                        .and(Sort.by("household.ho").ascending()));

        return billingRepository
                .findAllWithAdminFilter(dong, yearFrom, yearTo, month,
                        null, null, null, pageable)
                .map(BillingSummaryResponse::from);
    }

// ─────────────────────────────────────────────
// 관리자: 월별 전체 삭제 (실수 업로드 복구용)
//   - billing_detail → billing_log → billing 순 삭제
//   - 삭제 로그 1건 기록 (UPLOAD 로그로 기록, 감사용)
// ─────────────────────────────────────────────

    @Transactional
    public int deleteByBillingMonth(String billingMonth, String dong, Long adminId) {
        List<Billing> billings = (dong != null && !dong.isBlank())
                ? billingRepository.findAllByBillingMonthAndHousehold_Dong(billingMonth, dong)
                : billingRepository.findAllByBillingMonth(billingMonth);

        if (billings.isEmpty()) return 0;

        for (Billing b : billings) {
            billingDetailRepository.deleteByBilling_Id(b.getId());
        }
        billingRepository.deleteAll(billings);

        billingLogRepository.save(BillingLog.builder()
                .billing(null)
                .userId(adminId)
                .actionType(BillingActionType.UPLOAD)
                .build());

        return billings.size();

    }

    // ─────────────────────────────────────────────
    // 관리자: 미납 세대 목록 — PageRequest 직접 전달
    //   BillingPageController.unpaidPage() 에서 사용
    // ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<BillingSummaryResponse> getUnpaidList(
            String dong, Integer year, String month,
            BillingStatus status, boolean overdue, PageRequest pageable
    ) {
        String yearFrom      = (year != null && month == null) ? year + "-01" : null;
        String yearTo        = (year != null && month == null) ? year + "-12" : null;
        String overdueBefore = overdue
                ? YearMonth.now().minusMonths(3).toString()
                : null;
        // status=null이면 UNPAID 고정 (미납 관리 페이지 기본값)
        BillingStatus resolvedStatus = (status != null) ? status : BillingStatus.UNPAID;

        return billingRepository
                .findAllWithAdminFilter(dong, yearFrom, yearTo, month,
                        null, resolvedStatus, overdueBefore, pageable)
                .map(b -> BillingSummaryResponse.of(b, "—")); // TODO: CustomUserDetails 연동 후 교체
    }

    // ─────────────────────────────────────────────
    // 관리자: 미납 세대 목록 — API 파라미터 방식
    //   BillingApiController.getAdminUnpaid() 에서 사용
    // ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<BillingSummaryResponse> getAdminUnpaidList(
            Integer year, String month, String monthOnly,  // ★ monthOnly 추가
            String dong, String statusStr,
            Boolean overdueOnly, int size, int page
    ) {
        String yearFrom      = (year != null && month == null) ? year + "-01" : null;
        String yearTo        = (year != null && month == null) ? year + "-12" : null;
        String overdueBefore = Boolean.TRUE.equals(overdueOnly)
                ? YearMonth.now().minusMonths(3).toString()
                : null;

        // status 파라미터 처리
        // null 또는 "all" → status 조건 없음 (전체)
        // "UNPAID" → UNPAID만
        // "PAID"   → PAID만
        // overdueOnly=true → status 무관하게 기간 필터만 적용 (UNPAID 고정)
        BillingStatus status = null;
        if (Boolean.TRUE.equals(overdueOnly)) {
            status = BillingStatus.UNPAID;
        } else if ("UNPAID".equals(statusStr)) {
            status = BillingStatus.UNPAID;
        } else if ("PAID".equals(statusStr)) {
            status = BillingStatus.PAID;
        }
        // null이면 전체 조회

        PageRequest pageable = PageRequest.of(page, size,
                Sort.by("billingMonth").descending()
                        .and(Sort.by("household.dong").ascending())
                        .and(Sort.by("household.ho").ascending()));

        return billingRepository
                .findAllWithAdminFilter(dong, yearFrom, yearTo, month,
                        monthOnly, status, overdueBefore, pageable)  // ★ monthOnly 전달
                .map(b -> BillingSummaryResponse.of(b, "—"));
    }

    // ─────────────────────────────────────────────
    // 관리자: 납부완료 처리
    //   - Billing.status UNPAID → PAID
    //   - BillingLog에 STATUS_CHANGE 기록 (created_at이 실제 납부일)
    // ─────────────────────────────────────────────

    @Transactional
    public void markAsPaid(Long billingId, Long adminId) {
        Billing billing = billingRepository.findById(billingId)
                .orElseThrow(() -> new EntityNotFoundException("Billing not found: " + billingId));

        if (billing.getStatus() == BillingStatus.PAID) return;

        billingRepository.updateStatus(billingId, BillingStatus.PAID);

        billingLogRepository.save(BillingLog.builder()
                .billing(billing)
                .userId(adminId)
                .actionType(BillingActionType.STATUS_CHANGE)
                .build());
    }

    // ─────────────────────────────────────────────
    // 고지서 상세 (관리자 미리보기 + 입주민 모달 공통)
    // ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public BillingDetailResponse getBillingDetail(Long billingId) {
        Billing billing = billingRepository.findById(billingId)
                .orElseThrow(() -> new EntityNotFoundException("Billing not found: " + billingId));

        List<BillingDetail> details =
                billingDetailRepository.findByBilling_IdOrderBySortOrderAsc(billingId);

        return BillingDetailResponse.from(billing, details);
    }

    // ─────────────────────────────────────────────
    // 업로드: 부과월 중복 확인
    // ─────────────────────────────────────────────

    public boolean existsByBillingMonth(String billingMonth) {
        return billingRepository.existsByBillingMonth(billingMonth);
    }

    // ─────────────────────────────────────────────
    // 입주민: 관리비 목록 — PageRequest 직접 전달
    //   BillingPageController.billingPage() 에서 사용
    //   시그니처: getBillingList(householdId, yearFrom, yearTo, status, pageable)
    // ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<BillingSummaryResponse> getBillingList(
            Long householdId, String yearFrom, String yearTo,
            BillingStatus status, PageRequest pageable
    ) {
        return billingRepository
                .findByHouseholdIdWithFilter(householdId, yearFrom, yearTo, null, status, pageable)
                .map(BillingSummaryResponse::from);
    }

    // ─────────────────────────────────────────────
    // 입주민: 관리비 목록 — API 파라미터 방식
    //   BillingApiController.getResidentList() 에서 사용
    // ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<BillingSummaryResponse> getResidentBillingList(
            Long householdId, Integer year, String month,
            String statusStr, int size, int page
    ) {
        String yearFrom = (year != null && month == null) ? year + "-01" : null;
        String yearTo   = (year != null && month == null) ? year + "-12" : null;
        BillingStatus status = (statusStr != null && !statusStr.isBlank())
                ? BillingStatus.valueOf(statusStr) : null;

        PageRequest pageable = PageRequest.of(page, size,
                Sort.by("billingMonth").descending());

        return billingRepository
                .findByHouseholdIdWithFilter(householdId, yearFrom, yearTo, month, status, pageable)
                .map(BillingSummaryResponse::from);
    }

    // ─────────────────────────────────────────────
    // 입주민: 페이지 초기 데이터 (배너 + 요약 카드 + 최근 목록)
    //   BillingPageController.billingPage() 에서 사용
    //   ResidentBillingResponse 필드:
    //     hasUnpaid, latestUnpaidMonth, currentMonthAmount, unpaidCount, lastPaidDate, billings
    // ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ResidentBillingResponse getResidentBillingPage(Long householdId) {
        String currentMonth = YearMonth.now().toString(); // "2026-04"

        // 이번 달 청구액
        Optional<Billing> current = billingRepository
                .findByHousehold_IdAndBillingMonth(householdId, currentMonth);

        // 미납 건수
        int unpaidCount = billingRepository
                .countByHousehold_IdAndStatus(householdId, BillingStatus.UNPAID);

        // 미납 배너: 최신 미납 1건
        Optional<Billing> latestUnpaid = billingRepository
                .findLatestUnpaidByHouseholdId(householdId);

        boolean hasUnpaid = latestUnpaid.isPresent();

        // "2026-02" → "2026년 2월"
        String latestUnpaidMonth = latestUnpaid
                .map(b -> {
                    String[] parts = b.getBillingMonth().split("-");
                    return parts[0] + "년 " + Integer.parseInt(parts[1]) + "월";
                })
                .orElse(null);

        // 최근 납부일: BillingLog에서 STATUS_CHANGE 로그의 created_at 조회
        LocalDate lastPaidDate = billingLogRepository
                .findTopByBilling_Household_IdAndActionTypeOrderByCreatedAtDesc(
                        householdId, BillingActionType.STATUS_CHANGE)
                .map(log -> log.getCreatedAt().toLocalDate())
                .orElse(null);

        // 최근 3건 목록 (더보기 초기값)
        List<BillingSummaryResponse> billings = billingRepository
                .findByHouseholdIdWithFilter(
                        householdId, null, null, null, null,
                        PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "billingMonth")))
                .map(BillingSummaryResponse::from)
                .getContent();

        return ResidentBillingResponse.builder()
                .hasUnpaid(hasUnpaid)
                .latestUnpaidMonth(latestUnpaidMonth)
                .currentMonthAmount(current.map(Billing::getTotalAmount).orElse(null))
                .unpaidCount(unpaidCount)
                .lastPaidDate(lastPaidDate)
                .billings(billings)
                .build();
    }
}