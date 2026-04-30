package com.hometalk.onepass.billing.controller;

import com.hometalk.onepass.billing.dto.BillingSummaryResponse;
import com.hometalk.onepass.billing.dto.ResidentBillingResponse;
import com.hometalk.onepass.billing.entity.BillingStatus;
import jakarta.servlet.http.HttpServletRequest;
import com.hometalk.onepass.billing.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/billing")
@RequiredArgsConstructor
// @PreAuthorize("hasRole('RESIDENT')")
public class BillingResidentController {

    private final BillingService billingService;

    // ─────────────────────────────────────────────
    // TODO: Security 완성 후 CustomUserDetails로 교체
    private static final Long TEMP_HOUSEHOLD_ID = 7L;
    // ─────────────────────────────────────────────


    // ─────────────────────────────────────────────
    // 입주민 관리비 조회 페이지 (RESIDENT 전용)
    // ─────────────────────────────────────────────

    @GetMapping
    public String billingPage(Model model, HttpServletRequest request) {

        Long householdId = TEMP_HOUSEHOLD_ID;

        ResidentBillingResponse response = billingService.getResidentBillingPage(householdId);

        List<BillingSummaryResponse> unpaidList = billingService
                .getBillingList(householdId, null, null, BillingStatus.UNPAID,
                        PageRequest.of(0, 12, Sort.by(Sort.Direction.DESC, "billingMonth")))
                .getContent();

        model.addAttribute("currentUri",        request.getRequestURI());
        model.addAttribute("contextPath",        "/hometop");
        model.addAttribute("unpaidList",         unpaidList);
        model.addAttribute("unpaidMonths",       unpaidList.stream()
                .map(BillingSummaryResponse::getBillingMonth).toList());
        model.addAttribute("currentMonthAmount", response.getCurrentMonthAmount());
        model.addAttribute("currentMonthLabel",
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy년 M월")));
        model.addAttribute("unpaidCount",        response.getUnpaidCount());
        model.addAttribute("latestPaidDate",     response.getLastPaidDate() != null
                ? response.getLastPaidDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
                : null);
        model.addAttribute("latestPaidMonth",    response.getLastPaidDate() != null
                ? response.getLastPaidDate().format(DateTimeFormatter.ofPattern("M월"))
                : null);
        model.addAttribute("billings",           response.getBillings());
        model.addAttribute("hasMore",            false);
        model.addAttribute("householdId",        householdId);
        model.addAttribute("unitInfo",           "");
        model.addAttribute("menu",               "billing");
        return "billing/billing_resident";
    }
}