package com.hometalk.onepass.billing.controller;

import com.hometalk.onepass.billing.dto.BillingDetailResponse;
import com.hometalk.onepass.billing.dto.BillingSummaryResponse;
import com.hometalk.onepass.billing.service.BillingService;
import com.hometalk.onepass.billing.service.BillingService.AdminBillingStats;
import com.hometalk.onepass.billing.service.BillingService.AdminDashboardStats;
import com.hometalk.onepass.billing.service.BillingUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 관리비 REST API 컨트롤러
 * context-path: /hometop  →  실제 경로: /hometop/api/billing/...
 */
@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingApiController {

    private final BillingService       billingService;
    private final BillingUploadService billingUploadService;

    // ─────────────────────────────────────────────
    // 관리자: 고지서 전체 목록 (업로드 화면 DB 모드)
    //   GET /api/billing/admin/list
    // ─────────────────────────────────────────────

    @GetMapping("/admin/list")
    public ResponseEntity<Page<BillingSummaryResponse>> getAdminList(
            @RequestParam(required = false)     Integer year,
            @RequestParam(required = false)     String  month,
            @RequestParam(required = false)     String  dong,
            @RequestParam(defaultValue = "200") int     size,
            @RequestParam(defaultValue = "0")   int     page
    ) {
        return ResponseEntity.ok(
                billingService.getAdminBillingList(year, month, dong, size, page)
        );
    }

    // ─────────────────────────────────────────────
    // 관리자: 미납 세대 목록
    //   GET /api/billing/admin/unpaid
    // ─────────────────────────────────────────────

    @GetMapping("/admin/unpaid")
    public ResponseEntity<Page<BillingSummaryResponse>> getAdminUnpaid(
            @RequestParam(required = false)    Integer year,
            @RequestParam(required = false)    String  month,
            @RequestParam(required = false)    String  monthOnly,
            @RequestParam(required = false)    String  dong,
            @RequestParam(required = false)    String  status,
            @RequestParam(required = false)    Boolean overdueOnly,
            @RequestParam(defaultValue = "20") int     size,
            @RequestParam(defaultValue = "0")  int     page
    ) {
        return ResponseEntity.ok(
                billingService.getAdminUnpaidList(year, month, monthOnly, dong, status, overdueOnly, size, page)
        );
    }

    // ─────────────────────────────────────────────
    // 관리자: 미납 통계
    //   GET /api/billing/admin/stats?billingMonth=2026-03
    //   → AdminBillingStats { total, paid, unpaid, paidRate }
    // ─────────────────────────────────────────────

    @GetMapping("/admin/stats")
    public ResponseEntity<AdminBillingStats> getAdminStats(
            @RequestParam String billingMonth
    ) {
        return ResponseEntity.ok(billingService.getAdminStats(billingMonth));
    }
    // JS 동적 통계 갱신용
    @GetMapping("/admin/stats/dashboard")
    public ResponseEntity<AdminDashboardStats> getDashboardStats(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String  month,
            @RequestParam(required = false) String  dong
    ) {
        return ResponseEntity.ok(billingService.getDashboardStats(year, month, dong));
    }

    // ─────────────────────────────────────────────
    // 관리자: 납부완료 처리
    //   POST /api/billing/admin/{billingId}/pay?adminId=1
    // ─────────────────────────────────────────────

    @PostMapping("/admin/{billingId}/pay")
    public ResponseEntity<Void> markAsPaid(
            @PathVariable Long billingId,
            @RequestParam(defaultValue = "1") Long adminId   // TODO: CustomUserDetails로 교체
    ) {
        billingService.markAsPaid(billingId, adminId);
        return ResponseEntity.ok().build();
    }

    // ─────────────────────────────────────────────
    // 관리자: 일괄 납부완료 처리
    //   POST /api/billing/admin/pay/bulk
    //   Body: [1, 2, 3, ...]
    // ─────────────────────────────────────────────

    @PostMapping("/admin/pay/bulk")
    public ResponseEntity<Map<String, Integer>> markAsPaidBulk(
            @RequestBody  List<Long> billingIds,
            @RequestParam(defaultValue = "1") Long adminId
    ) {
        int count = 0;
        for (Long id : billingIds) {
            try {
                billingService.markAsPaid(id, adminId);
                count++;
            } catch (Exception e) {
                // 이미 처리된 건은 스킵
            }
        }
        return ResponseEntity.ok(Map.of("processed", count));
    }

    // ─────────────────────────────────────────────
    // 업로드: 부과월 중복 확인
    //   GET /api/billing/admin/upload/check?billingMonth=2026-03
    //   → { "exists": true/false }
    // ─────────────────────────────────────────────

    @GetMapping("/admin/upload/check")
    public ResponseEntity<Map<String, Boolean>> checkDuplicate(
            @RequestParam String billingMonth
    ) {
        return ResponseEntity.ok(
                Map.of("exists", billingService.existsByBillingMonth(billingMonth))
        );
    }

    // ─────────────────────────────────────────────
    // 업로드: 확정 저장
    //   POST /api/billing/admin/upload/confirm?adminId=1
    //   Body: List<UploadRow>
    //   → { insertCount, updateCount }
    // ─────────────────────────────────────────────

    @PostMapping("/admin/upload/confirm")
    public ResponseEntity<BillingUploadService.UploadConfirmResult> confirmUpload(
            @RequestBody  List<BillingUploadService.UploadRow> rows,
            @RequestParam(defaultValue = "1") Long adminId   // TODO: CustomUserDetails로 교체
    ) {
        return ResponseEntity.ok(billingUploadService.confirmUpload(rows, adminId));
    }
    // ─────────────────────────────────────────────
    // 관리자: 월별 전체 삭제 (실수 업로드 복구용)
    //   DELETE /api/billing/admin/month/2026-03?adminId=1
    //   → { "deleted": 120 }
    // ─────────────────────────────────────────────

    @DeleteMapping("/admin/month/{billingMonth}")
    public ResponseEntity<Map<String, Integer>> deleteByMonth(
            @PathVariable String billingMonth,
            @RequestParam(required = false) String dong,
            @RequestParam(defaultValue = "1") Long adminId
    ) {
        int deleted = billingService.deleteByBillingMonth(billingMonth, dong, adminId);
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }



    // ─────────────────────────────────────────────
    // 고지서 상세 (관리자 미리보기 + 입주민 고지서 모달 공통)
    //   GET /api/billing/{billingId}/detail
    // ─────────────────────────────────────────────

    @GetMapping("/{billingId}/detail")
    public ResponseEntity<BillingDetailResponse> getDetail(
            @PathVariable Long billingId
    ) {
        return ResponseEntity.ok(billingService.getBillingDetail(billingId));
    }

    // ─────────────────────────────────────────────
    // 입주민: 관리비 목록 (더보기 / 필터)
    //   GET /api/billing/resident/list
    // ─────────────────────────────────────────────

    @GetMapping("/resident/list")
    public ResponseEntity<Page<BillingSummaryResponse>> getResidentList(
            @RequestParam                       Long    householdId,  // TODO: CustomUserDetails로 교체
            @RequestParam(required = false)     Integer year,
            @RequestParam(required = false)     String  month,
            @RequestParam(required = false)     String  status,
            @RequestParam(defaultValue = "12")  int     size,
            @RequestParam(defaultValue = "0")   int     page
    ) {
        return ResponseEntity.ok(
                billingService.getResidentBillingList(householdId, year, month, status, size, page)
        );
    }
}