package com.hometalk.onepass.reservation.controller;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.facility.dto.FacilityRequestDto;
import com.hometalk.onepass.facility.dto.FacilityResponseDto;
import com.hometalk.onepass.facility.service.FacilityService;
import com.hometalk.onepass.reservation.dto.ReservationResponseDto;
import com.hometalk.onepass.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
       [입주민] 예약 신청 화면
     */
    @GetMapping("/apply")
    public String showApplyForm(Model model) {
        model.addAttribute("facilities", facilityService.findAll());
        return "reservation/reservation";
    }

    /*
       [입주민] 내 예약 목록
     */
    @GetMapping("/my")
    public String myReservations(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth";
        }
        Object principal = authentication.getPrincipal();
        Long userId;
        if (principal instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) principal;
            userId = userDetails.getUserId();
        } else if (principal instanceof org.springframework.security.core.userdetails.User) {
            return "redirect:/auth/logout";
        } else {
            return "redirect:/auth";
        }

        List<ReservationResponseDto> myRes = reservationService.findByUserId(userId);
        model.addAttribute("reservations", myRes);
        return "reservation/my-list";
    }

    /*
     *  예약 취소 처리
     */
    @PostMapping("/cancel/{id}")
    public String cancelReservation(@PathVariable Long id,
                                    @RequestHeader(value = "Referer", required = false) String referer,
                                    Authentication authentication) {

        if (authentication == null) return "redirect:/auth";

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        reservationService.cancel(
                id,
                userDetails.getUserId(),
                userDetails.getRole(),
                null
        );

        if (referer != null) {
            return "redirect:" + referer;
        }

        return "redirect:/reservation/my";
    }

    /*
     * [스태프] 시설 설정 및 관리 화면
     */
    // 시설 목록
    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ADMIN')")
    public String listPage(Model model) {
        model.addAttribute("facilities", facilityService.findAll());
        return "reservation/admin/facilityList";
    }

    /*
     [스태프] 전체 예약 현황 관리 화면
    */
    @GetMapping("/admin/status")
    @PreAuthorize("hasRole('ADMIN')")
    public String manageStatus(Model model, Authentication authentication) {
        if (authentication == null) return "redirect:/auth";
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        model.addAttribute("adminName", userDetails.getName());
        model.addAttribute("reservations", reservationService.findAllWithDetails());
        return "reservation/admin/reservation-status";
    }



    // 시설 등록 폼
    @GetMapping("/admin/facilities")
    @PreAuthorize("hasRole('ADMIN')")
    public String registerForm(Model model) {
        model.addAttribute("facility", null); // 새 등록임을 명시
        model.addAttribute("isEdit", false);
        return "reservation/admin/facility-admin";
    }

    // 시설 등록
    @PostMapping("/admin/facilities/register")
    @PreAuthorize("hasRole('ADMIN')")
    public String registerFacility(@ModelAttribute FacilityRequestDto dto,
                                   @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {
        // 1. 서비스 호출해서 저장
        facilityService.register(dto);
        // 2. 관리 목록 페이지로 리다이렉트
        return "redirect:/reservation/admin/list";
    }

    // 시설 수정 폼
    @GetMapping("/admin/edit/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String editPage(@PathVariable Long id, Model model) {
        FacilityResponseDto facility = facilityService.findOne(id);
        model.addAttribute("facility", facility);
        model.addAttribute("isEdit", true);
        return "reservation/admin/facility-admin";
    }

    // 시설 수정
    @PostMapping("/admin/facilities/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateFacility(@PathVariable Long id,
                                 @ModelAttribute FacilityRequestDto dto) {
        facilityService.update(id, dto);
        return "redirect:/reservation/admin/list";
    }

    // 시설 삭제
    @PostMapping("/admin/facilities/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteFacility(@PathVariable Long id) {
        // 1. 서비스 호출해서 DB 데이터 삭제
        facilityService.delete(id);
        // 2. 삭제 후 다시 관리 목록 페이지
        return "redirect:/reservation/admin/list";
    }


    // 캘린더 전용
    @GetMapping("/calendar")
    public String calendarPage() {
        return "reservation/calendar";
    }

}