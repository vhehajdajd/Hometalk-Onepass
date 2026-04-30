package com.hometalk.onepass.billing.controller;

import com.hometalk.onepass.billing.dto.BillingSummaryResponse;
import com.hometalk.onepass.billing.dto.ResidentBillingResponse;
import com.hometalk.onepass.billing.entity.BillingStatus;
import com.hometalk.onepass.billing.service.BillingService.AdminBillingStats;

import jakarta.servlet.http.HttpServletRequest;
import com.hometalk.onepass.billing.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/billing")
@RequiredArgsConstructor
public class BillingPageController {

    private final BillingService billingService;

    // ─────────────────────────────────────────────
    // 입주민 관리비 페이지
    // ─────────────────────────────────────────────

    @GetMapping
    public String billingPage(Model model, HttpServletRequest request) {
        // TODO: Security 완성 후 CustomUserDetails에서 추출
        Long householdId = 1L;

        ResidentBillingResponse response = billingService.getResidentBillingPage(householdId);

        // HTML 변수명에 맞춰 개별로 넘기기
        List<BillingSummaryResponse> unpaidList = billingService
                .getBillingList(householdId, null, null, BillingStatus.UNPAID,
                        PageRequest.of(0, 12, Sort.by(Sort.Direction.DESC, "billingMonth")))
                .getContent();

        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("contextPath", "/hometop");
        model.addAttribute("unpaidList",    unpaidList);
        model.addAttribute("unpaidMonths",  unpaidList.stream()
                .map(BillingSummaryResponse::getBillingMonth)
                .toList());
        model.addAttribute("currentMonthAmount", response.getCurrentMonthAmount());
        model.addAttribute("currentMonthLabel",
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy년 M월")));
        model.addAttribute("unpaidCount",   response.getUnpaidCount());
        model.addAttribute("latestPaidDate", response.getLastPaidDate() != null
                ? response.getLastPaidDate()
                .format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
                : null);
        model.addAttribute("latestPaidMonth", response.getLastPaidDate() != null
                ? response.getLastPaidDate()
                .format(DateTimeFormatter.ofPattern("M월"))
                : null);
        model.addAttribute("billings",    response.getBillings());
        model.addAttribute("hasMore",     false);
        model.addAttribute("householdId", householdId);
        model.addAttribute("unitInfo",    "");
        model.addAttribute("menu",        "billing");
        return "billing/billing_resident";
    }


    // ─────────────────────────────────────────────
    // 관리자 고지서 업로드 페이지
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
    // 관리자 미납 세대 관리 페이지
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
