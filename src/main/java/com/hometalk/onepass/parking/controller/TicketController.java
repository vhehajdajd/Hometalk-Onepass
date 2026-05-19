package com.hometalk.onepass.parking.controller;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.parking.dto.request.TicketApplyRequest;
import com.hometalk.onepass.parking.dto.request.TicketCancelRequest;
import com.hometalk.onepass.parking.dto.response.ParkingSearchResponse;
import com.hometalk.onepass.parking.dto.response.TicketResponse;
import com.hometalk.onepass.parking.service.TicketRegisterService;
import com.hometalk.onepass.parking.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/parking")
public class TicketController {

    private final TicketService ticketService;
    private final TicketRegisterService ticketRegisterService;

    @GetMapping("/ticket")
    public String ticketPage(@AuthenticationPrincipal CustomUserDetails userDetails,
                             Model model) {
        Long householdId = userDetails.getHouseholdId();
        List<TicketResponse> tickets = ticketService.getCurrentMonthTickets(householdId);
        model.addAttribute("tickets", tickets);
        return "parking/ticket-register";
    }

    @GetMapping("/ticket/current")
    @ResponseBody
    public ResponseEntity<List<TicketResponse>> getCurrentMonthTickets(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long householdId = userDetails.getHouseholdId();
        return ResponseEntity.ok(ticketService.getCurrentMonthTickets(householdId));
    }

    @GetMapping("/ticket/search")
    @ResponseBody
    public ResponseEntity<List<ParkingSearchResponse>> searchParkedVehicle(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String keyword) {
        Long householdId = userDetails.getHouseholdId();
        return ResponseEntity.ok(ticketRegisterService.searchParkedVehicleList(keyword, householdId));
    }

    @PostMapping("/ticket/apply")
    @ResponseBody
    public ResponseEntity<Void> applyTicket(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody TicketApplyRequest request) {
        Long householdId = userDetails.getHouseholdId();
        ticketRegisterService.applyTicket(request, householdId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/ticket/cancel")
    @ResponseBody
    public ResponseEntity<Void> cancelTicket(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody TicketCancelRequest request) {
        Long householdId = userDetails.getHouseholdId();
        ticketRegisterService.cancelTicket(request, householdId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/ticket/my-vehicles")
    @ResponseBody
    public ResponseEntity<List<ParkingSearchResponse>> getMyParkedVehicles(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long householdId = userDetails.getHouseholdId();
        return ResponseEntity.ok(ticketRegisterService.getMyParkedVehicles(householdId));
    }

    // POST /parking/ticket/unmatch
    @PostMapping("/ticket/unmatch")
    @ResponseBody
    public ResponseEntity<Void> unmatchHousehold(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody Map<String, Long> body) {
        Long parkingId = body.get("parkingId");
        if (parkingId == null) return ResponseEntity.badRequest().build();
        Long householdId = userDetails.getHouseholdId();
        ticketRegisterService.unmatchHousehold(parkingId, householdId);
        return ResponseEntity.ok().build();
    }
}