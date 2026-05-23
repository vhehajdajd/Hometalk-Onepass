package com.hometalk.onepass.parking.controller;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.parking.dto.request.VisitReservationRequest;
import com.hometalk.onepass.parking.dto.response.VisitReservationResponse;
import com.hometalk.onepass.parking.entity.VisitReservation;
import com.hometalk.onepass.parking.service.VisitReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.IntStream;

@Controller
@RequiredArgsConstructor
@RequestMapping("/parking")
public class VisitReservationController {

    private final VisitReservationService visitReservationService;

    @GetMapping("/visit")
    public String visitReservationPage(@AuthenticationPrincipal CustomUserDetails userDetails,
                                       Model model) {
        Long householdId = userDetails.getHouseholdId();
        List<VisitReservationResponse> reservations =
                visitReservationService.getHouseholdReservations(householdId);
        model.addAttribute("reservations", reservations);
        return "parking/visit-reservation";
    }

    @GetMapping("/visit/register")
    public String visitReservationRegisterPage(Model model) {
        addDateTimeAttributes(model);
        model.addAttribute("reservation", null);
        model.addAttribute("paths", List.of("parking", "reservationRegister", "reservationRegisterForm"));
        return "parking/visit-reservation-form";
    }

    @GetMapping("/visit/update/{reservationId}")
    public String visitReservationUpdatePage(@PathVariable Long reservationId, Model model) {
        VisitReservationResponse reservation = visitReservationService.getReservation(reservationId);
        model.addAttribute("reservation", reservation);
        model.addAttribute("paths", List.of("parking", "reservationRegister", "reservationRegisterForm"));
        addDateTimeAttributes(model);
        return "parking/visit-reservation-form";
    }

    @PostMapping("/visit/register")
    @ResponseBody
    public ResponseEntity<VisitReservationResponse> register(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody VisitReservationRequest request) {
        Long householdId = userDetails.getHouseholdId();
        return ResponseEntity.ok(visitReservationService.register(householdId, request));
    }

    @PostMapping("/visit/update/{reservationId}")
    @ResponseBody
    public ResponseEntity<VisitReservationResponse> update(
            @PathVariable Long reservationId,
            @RequestBody VisitReservationRequest request) {
        return ResponseEntity.ok(visitReservationService.update(reservationId, request));
    }

    @PostMapping("/visit/cancel/{reservationId}")
    @ResponseBody
    public ResponseEntity<Void> cancel(@PathVariable Long reservationId) {
        visitReservationService.cancel(reservationId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/visit/enter/{reservationId}")
    @ResponseBody
    public ResponseEntity<Void> enter(@PathVariable Long reservationId) {
        visitReservationService.enter(reservationId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/visit/pending")
    @ResponseBody
    public ResponseEntity<List<VisitReservationResponse>> getPendingConfirm(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long householdId = userDetails.getHouseholdId();
        return ResponseEntity.ok(visitReservationService.getPendingConfirmReservations(householdId));
    }

    @GetMapping("/visit/list")
    @ResponseBody
    public ResponseEntity<List<VisitReservationResponse>> getReservationsByStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) VisitReservation.ReservationStatus status) {
        Long householdId = userDetails.getHouseholdId();
        if (status != null) {
            return ResponseEntity.ok(
                    visitReservationService.getHouseholdReservationsByStatus(householdId, status));
        }
        return ResponseEntity.ok(visitReservationService.getHouseholdReservations(householdId));
    }

    private void addDateTimeAttributes(Model model) {
        model.addAttribute("years", List.of(2026, 2027, 2028));
        model.addAttribute("months", IntStream.rangeClosed(1, 12).boxed().toList());
        model.addAttribute("days", IntStream.rangeClosed(1, 31).boxed().toList());
        model.addAttribute("hours", IntStream.rangeClosed(0, 23).boxed().toList());
        model.addAttribute("minutes", List.of("00", "10", "20", "30", "40", "50"));
    }

    @GetMapping("/visit/manage")
    public String visitManagePage() {
        return "parking/visit-management";
    }
}