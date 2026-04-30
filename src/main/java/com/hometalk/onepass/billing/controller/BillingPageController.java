package com.hometalk.onepass.billing.controller;

import com.hometalk.onepass.billing.dto.AdminDashboardResponse;
import com.hometalk.onepass.billing.dto.BillingSummaryResponse;
import com.hometalk.onepass.billing.entity.BillingStatus;
import com.hometalk.onepass.billing.service.BillingService.AdminBillingStats;

import jakarta.servlet.http.HttpServletRequest;
import com.hometalk.onepass.billing.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Controller
@RequestMapping("/billing")
@RequiredArgsConstructor
// @PreAuthorize("hasRole('ADMIN')")
public class BillingPageController {

    private final BillingService billingService;

    // ─────────────────────────────────────────────────────────
    // 대시보드 - 관리자 특정 월의 '미납 총액' 합계
    // ─────────────────────────────────────────────────────────
    @GetMapping("/api/billing/admin/dashboard-summary")
    @ResponseBody
    public ResponseEntity<AdminDashboardResponse> getDashboardSummary() {
        AdminDashboardResponse summary = billingService.getAdminDashboardSummary();
        return ResponseEntity.ok(summary);
    }

    // ─────────────────────────────────────────────
    // 관리자 고지서 업로드 페이지 (ADMIN 전용)
    // ─────────────────────────────────────────────

    @GetMapping("/admin/upload")
    public String uploadPage(Model model, HttpServletRequest request) {
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("menu",        "billing");
        model.addAttribute("contextPath", "/hometop");
        return "billing/billing_admin_upload";
    }

    // 관리자 고지서 업로드 후 관리 페이지
    @GetMapping("/admin/monthly")
    public String monthlyPage(Model model, HttpServletRequest request) {
        model.addAttribute("currentUri",  request.getRequestURI());
        model.addAttribute("menu",        "billing");
        model.addAttribute("contextPath", "/hometop");
        return "billing/billing_admin_monthly";
    }

    // ─────────────────────────────────────────────
    // 관리자 미납 세대 관리 페이지 (ADMIN 전용)
    // ─────────────────────────────────────────────

    @GetMapping("/admin/unpaid")
    public String unpaidPage(
            @RequestParam(required = false) String dong,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) BillingStatus status,
            @RequestParam(defaultValue = "false") boolean overdue,
            @RequestParam(defaultValue = "0") int page,
            Model model, HttpServletRequest request
    ) {
        String currentMonth = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM"));
        AdminBillingStats stats = billingService.getAdminStats(currentMonth);

        Page<BillingSummaryResponse> unpaidPage = billingService.getUnpaidList(
                dong, year, month, status, overdue,
                PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "billingMonth"))
        );

        model.addAttribute("totalCount",  stats.total());
        model.addAttribute("paidCount",   stats.paid());
        model.addAttribute("unpaidCount", stats.unpaid());
        model.addAttribute("paidRate",    stats.paidRate());
        model.addAttribute("billingList", unpaidPage.getContent());
        model.addAttribute("currentPage", unpaidPage.getNumber());
        model.addAttribute("totalPages",  unpaidPage.getTotalPages());
        model.addAttribute("pageSize",    20);
        model.addAttribute("menu",        "billing");
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("contextPath", "/hometop");

        return "billing/billing_admin_unpaid";
    }

}
