package com.hometalk.onepass.billing.service;

import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.billing.dto.*;
import com.hometalk.onepass.billing.entity.Billing;
import com.hometalk.onepass.billing.entity.BillingActionType;
import com.hometalk.onepass.billing.entity.BillingDetail;
import com.hometalk.onepass.billing.entity.BillingLog;
import com.hometalk.onepass.billing.entity.BillingStatus;
import com.hometalk.onepass.billing.repository.BillingDetailRepository;
import com.hometalk.onepass.billing.repository.BillingLogRepository;
import com.hometalk.onepass.billing.repository.BillingRepository;
import com.hometalk.onepass.auth.repository.HouseholdRepository;
import com.hometalk.onepass.notification.entity.NotificationTargetRole;
import com.hometalk.onepass.notification.entity.NotificationType;
import com.hometalk.onepass.notification.publisher.NotificationPublisher;
import com.hometalk.onepass.notification.service.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BillingService {

    // ★ 필드는 반드시 클래스 상단에 모아서 선언
    private final BillingRepository       billingRepository;
    private final BillingDetailRepository billingDetailRepository;
    private final BillingLogRepository    billingLogRepository;
    private final HouseholdRepository     householdRepository;
    private final NotificationService     notificationService;
    private final NotificationPublisher   notificationPublisher;

    // ─────────────────────────────────────────────
    // 대시보드 - 관리자 특정 월의 '미납 총액' 합계
    // /admin/summary 에서 호출 (대시보드용)
    // ─────────────────────────────────────────────
    public AdminDashboardResponse getAdminUnpaidSummary() {
        LocalDate today = LocalDate.now();
        YearMonth refMonth = today.getDayOfMonth() > 10
                ? YearMonth.now()
                : YearMonth.now().minusMonths(1);
        String currentMonth = refMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        long unpaidCount = billingRepository.countByBillingMonthAndStatus(currentMonth, BillingStatus.UNPAID);
        Long unpaidSum = billingRepository.sumTotalAmountByBillingMonthAndStatus(currentMonth, BillingStatus.UNPAID);
        long totalAmount = (unpaidSum != null) ? unpaidSum : 0L;

        return AdminDashboardResponse.builder()
                .billingMonth(Integer.parseInt(currentMonth.substring(5, 7)) + "월")
                .unpaidHouseholds(unpaidCount)
                .totalUnpaidAmount(totalAmount)
                .build();
    }

    // ─────────────────────────────────────────────
    // 대시보드 - 입주민용 관리비 요약 (미납 우선 노출 로직)
    // ─────────────────────────────────────────────
// ─────────────────────────────────────────────
// 대시보드 - 입주민용 관리비 요약 (미납 우선 노출 로직)
// ─────────────────────────────────────────────
    public ResidentDashboardResponse getResidentDashboardSummary(Long householdId) {
        String currentMonth  = YearMonth.now().toString();
        String overdueBefore = YearMonth.now().minusMonths(3).toString();

        // 1. 3개월 이상 미납 먼저 조회 (가장 오래된 것)
        Optional<Billing> target = billingRepository
                .findTopByHousehold_IdAndStatusAndBillingMonthLessThanEqualOrderByBillingMonthAsc(
                        householdId, BillingStatus.UNPAID, overdueBefore);

        // 2. 없으면 당월 조회
        if (target.isEmpty()) {
            target = billingRepository
                    .findByHousehold_IdAndBillingMonth(householdId, currentMonth);
        }

        // 3. 없으면 가장 최근 고지서
        if (target.isEmpty()) {
            target = billingRepository
                    .findTopByHousehold_IdOrderByBillingMonthDesc(householdId);
        }

        if (target.isEmpty()) return null;

        Billing b = target.get();
        String displayMonth   = Integer.parseInt(b.getBillingMonth().substring(5, 7)) + "월";
        String formattedDueDate = b.getDueDate()
                .format(DateTimeFormatter.ofPattern("yyyy년 M월 d일"));
        boolean isOverdue = b.getStatus() == BillingStatus.UNPAID
                && b.getBillingMonth().compareTo(overdueBefore) <= 0;

        return ResidentDashboardResponse.builder()
                .billingMonth(displayMonth)
                .status(b.getStatus().name())
                .totalAmount(b.getTotalAmount())
                .dueDate(formattedDueDate)
                .overdue(isOverdue)
                .build();
    }
    // ─────────────────────────────────────────────
    // AdminBillingStats 관리자 상태
    // ─────────────────────────────────────────────
    public record AdminBillingStats(
            long   total,
            long   paid,
            long   unpaid,
            double paidRate
    ) {}

    @Transactional(readOnly = true)
    public AdminBillingStats getAdminStats(String billingMonth) {
        long total  = billingRepository.countDistinctHouseholdByBillingMonth(billingMonth);
        long paid   = billingRepository.countByBillingMonthAndStatus(billingMonth, BillingStatus.PAID);
        long unpaid = billingRepository.countByBillingMonthAndStatus(billingMonth, BillingStatus.UNPAID);
        double rate = total > 0 ? Math.round((double) paid / total * 1000.0) / 10.0 : 0.0;
        return new AdminBillingStats(total, paid, unpaid, rate);
    }

    // ─────────────────────────────────────────────
    // AdminDashboardStats 관리자 대시보드 상태
    // ─────────────────────────────────────────────
    public record AdminDashboardStats(
            long   totalHouseholds,
            long   paidCount,
            long   unpaidCount,
            double paidRate,
            long   unpaidAmount,
            long   globalUnpaidBillings,
            long   globalUnpaidHouseholds,
            long   globalOverdueHouseholds
    ) {}

    // /admin/stats/dashboard 에서 호출 (unpaid 페이지용)
    @Transactional(readOnly = true)
    public AdminDashboardStats getAdminDashboardStats(Integer year, String month, String dong) {
        long totalHouseholds = householdRepository.count();

        String yearFrom = (year != null && month == null) ? year + "-01" : null;
        String yearTo   = (year != null && month == null) ? year + "-12" : null;

        long paidCount   = billingRepository.countPaidWithFilter(dong, yearFrom, yearTo, month);
        long unpaidCount = billingRepository.countUnpaidWithFilter(dong, yearFrom, yearTo, month);
        double paidRate  = totalHouseholds > 0
                ? Math.round((double) paidCount / totalHouseholds * 1000.0) / 10.0
                : 0.0;

        // 납부기한(익월 10일) 기준: 오늘이 10일 이후면 당월, 이전이면 전월
        LocalDate today = LocalDate.now();
        YearMonth refMonth = today.getDayOfMonth() > 10
                ? YearMonth.now()
                : YearMonth.now().minusMonths(1);
        String refMonthStr = refMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        Long unpaidSum;
        if (month != null) {
            // 월 필터 선택 시 해당 월 기준
            unpaidSum = billingRepository.sumTotalAmountByBillingMonthAndStatus(
                    month, BillingStatus.UNPAID);
        } else {
            // 연도만 선택이거나 필터 없을 때 → 항상 기준월 고정
            unpaidSum = billingRepository.sumTotalAmountByBillingMonthAndStatus(
                    refMonthStr, BillingStatus.UNPAID);
        }
        long unpaidAmount = unpaidSum != null ? unpaidSum : 0L;

        String overdueBefore = YearMonth.now().minusMonths(3).toString();
        long globalUnpaidBillings    = billingRepository.countAllUnpaid();
        long globalUnpaidHouseholds  = billingRepository.countDistinctUnpaidHouseholds();
        long globalOverdueHouseholds = billingRepository.countDistinctOverdueHouseholds(overdueBefore);

        return new AdminDashboardStats(
                totalHouseholds, paidCount, unpaidCount, paidRate,
                unpaidAmount, globalUnpaidBillings, globalUnpaidHouseholds, globalOverdueHouseholds
        );
    }

    // ─────────────────────────────────────────────
    // 관리자: 고지서 전체 목록
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
                .findAllWithAdminFilter(dong, yearFrom, yearTo, month, null, null, null, pageable)
                .map(BillingSummaryResponse::from);
    }

    // ─────────────────────────────────────────────
    // 관리자: 월별 전체 삭제
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
    // ─────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<BillingSummaryResponse> getUnpaidList(
            String dong, Integer year, String month,
            BillingStatus status, boolean overdue, PageRequest pageable
    ) {
        String yearFrom      = (year != null && month == null) ? year + "-01" : null;
        String yearTo        = (year != null && month == null) ? year + "-12" : null;
        String overdueBefore = overdue ? YearMonth.now().minusMonths(3).toString() : null;
        BillingStatus resolvedStatus = (status != null) ? status : BillingStatus.UNPAID;

        return billingRepository
                .findAllWithAdminFilter(dong, yearFrom, yearTo, month, null, resolvedStatus, overdueBefore, pageable)
                .map(b -> BillingSummaryResponse.of(b, "—"));
    }

    // ─────────────────────────────────────────────
    // 관리자: 미납 세대 목록 — API 파라미터 방식
    // ─────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<BillingSummaryResponse> getAdminUnpaidList(
            Integer year, String month, String monthOnly,
            String dong, String statusStr,
            Boolean overdueOnly, int size, int page
    ) {
        String yearFrom      = (year != null && month == null) ? year + "-01" : null;
        String yearTo        = (year != null && month == null) ? year + "-12" : null;
        String overdueBefore = Boolean.TRUE.equals(overdueOnly)
                ? YearMonth.now().minusMonths(3).toString() : null;

        BillingStatus status = null;
        if (Boolean.TRUE.equals(overdueOnly)) {
            status = BillingStatus.UNPAID;
        } else if ("UNPAID".equals(statusStr)) {
            status = BillingStatus.UNPAID;
        } else if ("PAID".equals(statusStr)) {
            status = BillingStatus.PAID;
        }

        PageRequest pageable = PageRequest.of(page, size,
                Sort.by("billingMonth").descending()
                        .and(Sort.by("household.dong").ascending())
                        .and(Sort.by("household.ho").ascending()));

        return billingRepository
                .findAllWithAdminFilter(dong, yearFrom, yearTo, month, monthOnly, status, overdueBefore, pageable)
                .map(b -> BillingSummaryResponse.of(b, "—"));
    }

    // ─────────────────────────────────────────────
    // 관리자: 납부완료 처리
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

        // ── 알림 연동 (V5) ──────────────────────────────────────
        List<User> residents = billing.getHousehold().getUsers().stream()
                .filter(u -> u.getRole() == User.UserRole.RESIDENT)
                .toList();

        for (User resident : residents) {
            Long residentUserId = resident.getId();

            notificationService.markBroadcastAsRead(
                    NotificationType.BILLING_UPLOAD, residentUserId, NotificationTargetRole.RESIDENT);
            notificationService.deleteByTypeAndUser(NotificationType.BILLING_UNPAID,  residentUserId);
            notificationService.deleteByTypeAndUser(NotificationType.BILLING_OVERDUE, residentUserId);

            notificationPublisher.publish(
                    residentUserId,
                    NotificationTargetRole.RESIDENT,
                    NotificationType.BILLING_PAID,
                    "관리비 납부 완료",
                    billing.getBillingMonth() + " 관리비가 납부 처리되었습니다.",
                    "/billing",
                    billing.getId()
            );
        }
    }

    // ─────────────────────────────────────────────
    // 고지서 상세
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

        PageRequest pageable = PageRequest.of(page, size, Sort.by("billingMonth").descending());

        return billingRepository
                .findByHouseholdIdWithFilter(householdId, yearFrom, yearTo, month, status, pageable)
                .map(BillingSummaryResponse::from);
    }

    // ─────────────────────────────────────────────
    // 입주민: 페이지 초기 데이터
    // ─────────────────────────────────────────────
    @Transactional(readOnly = true)
    public ResidentBillingResponse getResidentBillingPage(Long householdId) {
        String currentMonth = YearMonth.now().toString();

        Optional<Billing> current = billingRepository
                .findByHousehold_IdAndBillingMonth(householdId, currentMonth);

        int unpaidCount = billingRepository
                .countByHousehold_IdAndStatus(householdId, BillingStatus.UNPAID);

        Optional<Billing> latestUnpaid = billingRepository
                .findLatestUnpaidByHouseholdId(householdId);

        boolean hasUnpaid = latestUnpaid.isPresent();

        String latestUnpaidMonth = latestUnpaid
                .map(b -> {
                    String[] parts = b.getBillingMonth().split("-");
                    return parts[0] + "년 " + Integer.parseInt(parts[1]) + "월";
                })
                .orElse(null);

        LocalDate lastPaidDate = billingLogRepository
                .findTopByBilling_Household_IdAndActionTypeOrderByCreatedAtDesc(
                        householdId, BillingActionType.STATUS_CHANGE)
                .map(log -> log.getCreatedAt().toLocalDate())
                .orElse(null);

        // 납부한 건 월 문구
        String lastPaidBillingMonth = billingLogRepository
                .findTopByBilling_Household_IdAndActionTypeOrderByCreatedAtDesc(
                        householdId, BillingActionType.STATUS_CHANGE)
                .map(log -> {
                    String bm = log.getBilling().getBillingMonth(); // "2026-04"
                    return Integer.parseInt(bm.split("-")[1]) + "월 ";
                })
                .orElse(null);

        List<BillingSummaryResponse> billings = billingRepository
                .findByHouseholdIdWithFilter(
                        householdId, null, null, null, null,
                        PageRequest.of(0, 12, Sort.by(Sort.Direction.DESC, "billingMonth")))
                .map(BillingSummaryResponse::from)
                .getContent();

        return ResidentBillingResponse.builder()
                .hasUnpaid(hasUnpaid)
                .latestUnpaidMonth(latestUnpaidMonth)
                .currentMonthAmount(current.map(Billing::getTotalAmount).orElse(null))
                .unpaidCount(unpaidCount)
                .lastPaidDate(lastPaidDate)
                .lastPaidBillingMonth(lastPaidBillingMonth)
                .billings(billings)
                .build();
    }
}