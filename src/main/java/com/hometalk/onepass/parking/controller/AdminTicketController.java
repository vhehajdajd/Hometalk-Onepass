package com.hometalk.onepass.parking.controller;

import com.hometalk.onepass.auth.entity.Household;
import com.hometalk.onepass.auth.repository.HouseholdRepository;
import com.hometalk.onepass.parking.entity.ParkingTicket;
import com.hometalk.onepass.parking.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminTicketController {

    private final TicketService ticketService;
    private final HouseholdRepository householdRepository;

    // ─── 관리자 티켓 발급 페이지 ─────────────────────────────────
    @GetMapping("/ticket")
    public String adminTicketPage(Model model) {
        List<Household> households = householdRepository.findAll();
        model.addAttribute("households", households);
        return "parking/admin-ticket";
    }

    // ─── 관리자 수동 티켓 발급 ───────────────────────────────────
    @PostMapping("/ticket/issue")
    @ResponseBody
    public ResponseEntity<Void> issueTicket(
            @RequestParam Long householdId,
            @RequestParam ParkingTicket.TicketType type,
            @RequestParam int totalCount) {

        ticketService.issueTicket(householdId, type, totalCount, LocalDate.now());
        return ResponseEntity.ok().build();
    }

    // ─── 관리자 전체 세대 월별 티켓 수동 발급 ───────────────────
    @PostMapping("/ticket/issue/monthly")
    @ResponseBody
    public ResponseEntity<Void> issueMonthlyTickets() {
        ticketService.issueMonthlyTickets();
        return ResponseEntity.ok().build();
    }
}