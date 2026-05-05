package com.hometalk.onepass.reservation.controller;

import com.hometalk.onepass.facility.dto.FacilityRequestDto;
import com.hometalk.onepass.facility.service.FacilityService;
import com.hometalk.onepass.reservation.dto.ReservationResponseDto;
import com.hometalk.onepass.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/reservation")
public class ReservationViewController {

    private final FacilityService facilityService;
    private final ReservationService reservationService;

    /*
     * [입주민] 예약 신청 화면
     * 파일 트리 확인 결과: templates/reservation/reservation.html
     */
    @GetMapping("/apply")
    public String showApplyForm(Model model) {
        model.addAttribute("facilities", facilityService.findAll());
        return "reservation/reservation"; // 👈 파일명에 맞춰 수정
    }

    /*
     * [입주민] 내 예약 목록
     * 파일 트리 확인 결과: templates/reservation/my-list.html (있다고 가정)
     */
// ReservationViewController.java (예시)
    @GetMapping("/my")
    public String myReservations(Model model) {
        Long currentUserId = 1L; // 👈 현재는 강제로 1번 세팅, 나중에 로그인 연동 시 수정
        List<ReservationResponseDto> myRes = reservationService.findByUserId(currentUserId);
        model.addAttribute("reservations", myRes); // 여기서 보낸 이름이 HTML의 reservations와 일치해야 함
        return "reservation/my-list";
    }

    /*
     * [스태프] 시설 설정 및 관리 화면
     */
    @GetMapping("/admin/facilities")
    public String manageFacilities(Model model) {
        model.addAttribute("facilities", facilityService.findAll());
        return "reservation/admin/facility-admin"; // 👈 경로에 reservation/admin/ 추가
    }

    // 시설 등록
    @PostMapping("/admin/facilities/register")
    public String registerFacility(@ModelAttribute FacilityRequestDto dto,
                                   @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {
        // 1. 서비스 호출해서 저장
        facilityService.register(dto);

        // 2. 관리 목록 페이지로 리다이렉트 (이제 정상 작동함)
        return "redirect:/reservation/admin/facilities";
    }

    // 시설 삭제
    @PostMapping("/admin/facilities/{id}/delete")
    public String deleteFacility(@PathVariable("id") Long id) {
        // 1. 서비스 호출해서 DB 데이터 삭제
        facilityService.delete(id);

        // 2. 삭제 후 다시 관리 목록 페이지로 리다이렉트
        return "redirect:/reservation/admin/facilities";
    }

    /*   [스태프] 전체 예약 현황 관리 화면
     * 파일 트리 확인 결과: templates/reservation/admin/reservation-statu.html (오타 수정 필요)
     */
    @GetMapping("/admin/status")
    public String manageStatus(Model model) {
        model.addAttribute("reservations", reservationService.findAllWithDetails());
        // 파일명을 reservation-status.html로 수정하신 후 아래 경로를 사용하세요.
        return "reservation/admin/reservation-status"; // 👈 경로 수정 및 오타 정정
    }


    // 캘린더 전용
    @GetMapping("/calendar")
    public String calendarPage() {
        return "reservation/calendar";
    }

}