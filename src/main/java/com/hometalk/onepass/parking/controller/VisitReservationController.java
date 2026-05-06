package com.hometalk.onepass.parking.controller;

import com.hometalk.onepass.parking.dto.request.VisitReservationRequest;
import com.hometalk.onepass.parking.dto.response.VisitReservationResponse;
import com.hometalk.onepass.parking.entity.VisitReservation;
import com.hometalk.onepass.parking.service.VisitReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    // 방문 예약 목록 페이지
    @GetMapping("/visit")
    public String visitReservationPage(Model model) {
        Long householdId = 1L;          // TODO: JWT 연동 후 추출
        List<VisitReservationResponse> reservations =
                visitReservationService.getHouseholdReservations(householdId);
        model.addAttribute("reservations", reservations);
        return "parking/visit-reservation";
    }

    // 방문 예약 등록 페이지
    @GetMapping("/visit/register")
    public String visitReservationRegisterPage(Model model) {
        addDateTimeAttributes(model);
        model.addAttribute("reservation", null);
        return "parking/visit-reservation-form";
    }

    // 방문 예약 수정 페이지
    @GetMapping("/visit/update/{reservationId}")
    public String visitReservationUpdatePage(@PathVariable Long reservationId, Model model) {
        VisitReservationResponse reservation = visitReservationService.getReservation(reservationId);
        model.addAttribute("reservation", reservation);
        addDateTimeAttributes(model);
        return "parking/visit-reservation-form";
    }

    // 방문 예약 등록 처리 (JSON)
    @PostMapping("/visit/register/{householdId}")
    @ResponseBody
    public ResponseEntity<VisitReservationResponse> register(
            @PathVariable Long householdId,
            @RequestBody VisitReservationRequest request) {
        if (householdId == null) {
            throw new IllegalArgumentException("householdId는 필수입니다.");
        }
        return ResponseEntity.ok(visitReservationService.register(householdId, request));
    }

    // 방문 예약 수정 처리 (JSON)
    @PostMapping("/visit/update/{reservationId}")
    @ResponseBody
    public ResponseEntity<VisitReservationResponse> update(
            @PathVariable Long reservationId,
            @RequestBody VisitReservationRequest request) {
        return ResponseEntity.ok(visitReservationService.update(reservationId, request));
    }

    // 방문 예약 취소
    @PostMapping("/visit/cancel/{reservationId}")
    @ResponseBody
    public ResponseEntity<Void> cancel(@PathVariable Long reservationId) {
        visitReservationService.cancel(reservationId);
        return ResponseEntity.ok().build();
    }

    // 입차 처리
    @PostMapping("/visit/enter/{reservationId}")
    @ResponseBody
    public ResponseEntity<Void> enter(@PathVariable Long reservationId) {
        visitReservationService.enter(reservationId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/visit/pending/{householdId}")
    @ResponseBody
    public ResponseEntity<List<VisitReservationResponse>> getPendingConfirm(
            @PathVariable Long householdId) {
        return ResponseEntity.ok(visitReservationService.getPendingConfirmReservations(householdId));
    }

    // 상태별 예약 목록 조회 (JSON)
    @GetMapping("/visit/list/{householdId}")
    @ResponseBody
    public ResponseEntity<List<VisitReservationResponse>> getReservationsByStatus(
            @PathVariable Long householdId,
            @RequestParam(required = false) VisitReservation.ReservationStatus status) {
        if (status != null) {
            return ResponseEntity.ok(
                    visitReservationService.getHouseholdReservationsByStatus(householdId, status));
        }
        return ResponseEntity.ok(visitReservationService.getHouseholdReservations(householdId));
    }

    // 날짜 select용 데이터 공통 메서드
    private void addDateTimeAttributes(Model model) {
        model.addAttribute("years", List.of(2025, 2026));
        model.addAttribute("months", IntStream.rangeClosed(1, 12).boxed().toList());
        model.addAttribute("days", IntStream.rangeClosed(1, 31).boxed().toList());
        model.addAttribute("hours", IntStream.rangeClosed(0, 23).boxed().toList());
        model.addAttribute("minutes", List.of(0, 10, 20, 30, 40, 50));
    }

    // 방문 차량 관리 페이지
    @GetMapping("/visit/manage")
    public String visitManagePage() {
        return "parking/visit-management";
    }
}