package com.hometalk.onepass.reservation.controller;

import com.hometalk.onepass.facility.service.FacilityService;
import com.hometalk.onepass.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/reservation")
public class ReservationViewController {

    private final FacilityService facilityService;
    private final ReservationService reservationService;

    /**
     * [입주민] 예약 신청 화면
     * 파일 트리 확인 결과: templates/reservation/reservation.html
     */
    @GetMapping("/apply")
    public String showApplyForm(Model model) {
        model.addAttribute("facilities", facilityService.findAll());
        return "reservation/reservation"; // 👈 파일명에 맞춰 수정
    }

    /**
     * [입주민] 내 예약 목록
     * 파일 트리 확인 결과: templates/reservation/my-list.html (있다고 가정)
     */
    @GetMapping("/my")
    public String myReservations() {
        return "reservation/my-list";
    }

    /**
     * [스태프] 시설 설정 및 관리 화면
     * 파일 트리 확인 결과: templates/reservation/admin/facility-admin.html
     */
    @GetMapping("/admin/facilities")
    public String manageFacilities(Model model) {
        model.addAttribute("facilities", facilityService.findAll());
        return "reservation/admin/facility-admin"; // 👈 경로에 reservation/admin/ 추가
    }

    /** * [스태프] 전체 예약 현황 관리 화면
     * 파일 트리 확인 결과: templates/reservation/admin/reservation-statu.html (오타 수정 필요)
     */
    @GetMapping("/admin/status")
    public String manageStatus(Model model) {
        model.addAttribute("reservations", reservationService.findAllWithDetails());
        // 파일명을 reservation-status.html로 수정하신 후 아래 경로를 사용하세요.
        return "reservation/admin/reservation-status"; // 👈 경로 수정 및 오타 정정
    }
}