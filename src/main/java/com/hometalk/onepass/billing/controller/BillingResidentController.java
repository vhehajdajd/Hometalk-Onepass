package com.hometalk.onepass.billing.controller;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.billing.dto.BillingSummaryResponse;
import com.hometalk.onepass.billing.dto.ResidentBillingResponse;
import com.hometalk.onepass.billing.entity.BillingStatus;
import jakarta.servlet.http.HttpServletRequest;
import com.hometalk.onepass.billing.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
public class BillingResidentController {

    private final BillingService billingService;

    @GetMapping
    public String billingPage(Model model, HttpServletRequest request,
                              @AuthenticationPrincipal CustomUserDetails user) {

        // ✅ CustomUserDetails에서 householdId 추출
        Long householdId = user.getHouseholdId();

        ResidentBillingResponse response = billingService.getResidentBillingPage(householdId);

        LocalDate today = LocalDate.now();

        List<BillingSummaryResponse> unpaidList = billingService
                .getBillingList(householdId, null, null, BillingStatus.UNPAID,
                        PageRequest.of(0, 12, Sort.by(Sort.Direction.DESC, "billingMonth")))
                .getContent()
                .stream()
                .filter(b -> b.getDueDate() != null && b.getDueDate().isBefore(today))
                .toList();

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
        model.addAttribute("latestPaidMonth",    response.getLastPaidBillingMonth());
        model.addAttribute("billings",           response.getBillings());
        model.addAttribute("hasMore",            response.getBillings().size() == 12);
        model.addAttribute("householdId",        householdId);
        model.addAttribute("unitInfo",           "");
        model.addAttribute("menu",               "billing");
        return "billing/billing_resident";
    }
}