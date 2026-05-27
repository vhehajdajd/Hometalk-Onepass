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

    // 방문 예약 목록 페이지
    @GetMapping("/visit")
    public String visitReservationPage(@AuthenticationPrincipal CustomUserDetails userDetails,
                                       Model model) {
        Long householdId = userDetails.getHouseholdId();
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
        model.addAttribute("paths", List.of("parking", "reservationRegister", "reservationRegisterForm"));
        return "parking/visit-reservation-form";
    }

    // 방문 예약 수정 페이지
    @GetMapping("/visit/update/{reservationId}")
    public String visitReservationUpdatePage(@PathVariable Long reservationId, Model model) {
        VisitReservationResponse reservation = visitReservationService.getReservation(reservationId);
        model.addAttribute("reservation", reservation);
        model.addAttribute("paths", List.of("parking", "reservationRegister", "reservationRegisterForm"));
        addDateTimeAttributes(model);
        return "parking/visit-reservation-form";
    }

    // 방문 예약 등록 처리
    @PostMapping("/visit/register")
    @ResponseBody
    public ResponseEntity<VisitReservationResponse> register(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody VisitReservationRequest request) {
        Long householdId = userDetails.getHouseholdId();
        return ResponseEntity.ok(visitReservationService.register(householdId, request));
    }

    // 방문 예약 수정 처리
    // ✅ 보안 수정: @AuthenticationPrincipal 추가 → Service에서 내 세대 예약인지 검증
    @PostMapping("/visit/update/{reservationId}")
    @ResponseBody
    public ResponseEntity<VisitReservationResponse> update(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long reservationId,
            @RequestBody VisitReservationRequest request) {
        Long householdId = userDetails.getHouseholdId();
        return ResponseEntity.ok(visitReservationService.update(reservationId, request, householdId));
    }

    // 방문 예약 취소
    // ✅ 보안 수정: @AuthenticationPrincipal 추가 → Service에서 내 세대 예약인지 검증
    @PostMapping("/visit/cancel/{reservationId}")
    @ResponseBody
    public ResponseEntity<Void> cancel(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long reservationId) {
        Long householdId = userDetails.getHouseholdId();
        visitReservationService.cancel(reservationId, householdId);
        return ResponseEntity.ok().build();
    }

    // 방문 예약 입차 처리 (스태프 입차 시 예약 상태 변경용)
    @PostMapping("/visit/enter/{reservationId}")
    @ResponseBody
    public ResponseEntity<Void> enter(@PathVariable Long reservationId) {
        visitReservationService.enter(reservationId);
        return ResponseEntity.ok().build();
    }

    // 수동 입차 대기 목록 (PENDING_CONFIRM) - 알림 클릭 시 진입
    @GetMapping("/visit/pending")
    @ResponseBody
    public ResponseEntity<List<VisitReservationResponse>> getPendingConfirm(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long householdId = userDetails.getHouseholdId();
        return ResponseEntity.ok(visitReservationService.getPendingConfirmReservations(householdId));
    }

    // 상태별 예약 목록 조회 (AJAX 탭 전환 시)
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

    // 날짜/시간 select 옵션 데이터 공통 세팅 (등록/수정 폼 공용)
    private void addDateTimeAttributes(Model model) {
        model.addAttribute("years", List.of(2026, 2027, 2028));
        model.addAttribute("months", IntStream.rangeClosed(1, 12).boxed().toList());
        model.addAttribute("days", IntStream.rangeClosed(1, 31).boxed().toList());
        model.addAttribute("hours", IntStream.rangeClosed(0, 23).boxed().toList());
        model.addAttribute("minutes", List.of("00", "10", "20", "30", "40", "50"));
    }

    // 방문 차량 관리 페이지 (미등록 차량 세대 등록)
    @GetMapping("/visit/manage")
    public String visitManagePage() {
        return "parking/visit-management";
    }
}