package com.hometalk.onepass.parking.controller;

import com.hometalk.onepass.parking.dto.request.TicketApplyRequest;
import com.hometalk.onepass.parking.dto.request.TicketCancelRequest;
import com.hometalk.onepass.parking.dto.response.ParkingSearchResponse;
import com.hometalk.onepass.parking.dto.response.TicketResponse;
import com.hometalk.onepass.parking.service.TicketRegisterService;
import com.hometalk.onepass.parking.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/parking")
public class TicketController {

    private final TicketService ticketService;
    private final TicketRegisterService ticketRegisterService;

    // ─── 입주자 티켓 조회 페이지 ─────────────────────────────────
    @GetMapping("/ticket")
    public String ticketPage(Model model) {
        Long householdId = 1L;
        List<TicketResponse> tickets = ticketService.getCurrentMonthTickets(householdId);
        model.addAttribute("tickets", tickets);
        return "parking/ticket-register";
    }

    // ─── 입주자 이번 달 티켓 조회 (JSON) ────────────────────────
    @GetMapping("/ticket/current")
    @ResponseBody
    public ResponseEntity<List<TicketResponse>> getCurrentMonthTickets() {
        Long householdId = 1L;
        return ResponseEntity.ok(ticketService.getCurrentMonthTickets(householdId));
    }

    // ─── 차량 조회 (주차 시간 + 티켓 현황) ──────────────────────
    @GetMapping("/ticket/search")
    @ResponseBody
    public ResponseEntity<List<ParkingSearchResponse>> searchParkedVehicle(
            @RequestParam String keyword) {

        Long householdId = 1L;
        List<ParkingSearchResponse> response =
                ticketRegisterService.searchParkedVehicleList(keyword, householdId);
        return ResponseEntity.ok(response);
    }

    // ─── 티켓 적용 ───────────────────────────────────────────────
    @PostMapping("/ticket/apply")
    @ResponseBody
    public ResponseEntity<Void> applyTicket(@RequestBody TicketApplyRequest request) {
        Long householdId = 1L;
        ticketRegisterService.applyTicket(request, householdId);
        return ResponseEntity.ok().build();
    }

    // ─── 티켓 취소 ───────────────────────────────────────────────
    @PostMapping("/ticket/cancel")
    @ResponseBody
    public ResponseEntity<Void> cancelTicket(@RequestBody TicketCancelRequest request) {
        Long householdId = 1L;
        ticketRegisterService.cancelTicket(request, householdId);
        return ResponseEntity.ok().build();
    }
}