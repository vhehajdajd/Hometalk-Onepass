package com.hometalk.onepass.reservation.controller;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.community.service.FileService;
import com.hometalk.onepass.facility.dto.FacilityRequestDto;
import com.hometalk.onepass.facility.dto.FacilityResponseDto;
import com.hometalk.onepass.facility.service.FacilityService;
import com.hometalk.onepass.reservation.dto.ReservationRequestDto;
import com.hometalk.onepass.reservation.dto.ReservationResponseDto;
import com.hometalk.onepass.reservation.entity.Reservation;
import com.hometalk.onepass.reservation.entity.ReservationStatus;
import com.hometalk.onepass.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/reservation")
public class ReservationViewController {

    private final FacilityService facilityService;
    private final ReservationService reservationService;
    private final FileService fileService;

    /*
       [입주민] 예약 신청 화면
     */
    @GetMapping("/apply")
    public String showApplyForm(Model model, Authentication authentication) {
        model.addAttribute("facilities", facilityService.findAll());
        Long userId = 1L;
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            userId = userDetails.getUserId();
        }
        List<FacilityResponseDto> facilities = facilityService.getFacilitiesWithStatus(userId);
        model.addAttribute("facilities", facilities);
        return "reservation/reservation";
    }

    // 예약 등록 처리
    /*
    @PostMapping("/register")
    public String registerReservation(@ModelAttribute ReservationRequestDto dto,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            reservationService.register(dto, userDetails.getUserId());
            redirectAttributes.addFlashAttribute("message", "예약이 완료되었습니다.");
            return "redirect:/reservation/my";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("status", "error");
            return "redirect:/reservation/apply";
        }
    }
    */

    /*
       [입주민] 내 예약 목록
     */
    @GetMapping("/my")
    public String myReservations(Model model, Authentication authentication,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "regDate") String sortBy,
                                 @RequestParam(required = false) ReservationStatus status) {
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

        Page<ReservationResponseDto> myResPage = reservationService.findByUserId(userId, page, 15, sortBy, status);

        model.addAttribute("reservations", myResPage);
        model.addAttribute("currentSort", sortBy);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", ReservationStatus.values());

        return "reservation/my-list";
    }

    /*
     *  예약 취소 처리
     */
    @PostMapping("/cancel/{id}")
    public String cancelReservation(@PathVariable Long id,
                                    @RequestParam(required = false) String reason,
                                    @RequestHeader(value = "Referer", required = false) String referer,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {

        if (authentication == null) return "redirect:/auth";

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        try {
            reservationService.cancel(id, userDetails.getUserId(), userDetails.getRole(), reason);
            redirectAttributes.addFlashAttribute("message", "예약 취소가 완료되었습니다.");
            redirectAttributes.addFlashAttribute("status", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("status", "error");
        }

        return (referer != null) ? "redirect:" + referer : "redirect:/reservation/my";
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
    public String manageStatus(Model model, Authentication authentication,
                               @RequestParam(required = false) ReservationStatus status,
                               @RequestParam(defaultValue = "0") int page) {
        if (authentication == null) return "redirect:/auth";
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Page<ReservationResponseDto> adminResPage = reservationService.findAllFiltered(status, page, 15);
        Map<String, Long> stats = reservationService.getAdminDashboardStats();

        model.addAttribute("selectedStatus", status); // 현재 선택된 필터 유지용
        model.addAttribute("statuses", ReservationStatus.values()); // 드롭다운 목록용
        model.addAllAttributes(stats);
        model.addAttribute("adminName", userDetails.getName());
        model.addAttribute("reservations", adminResPage);
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
                                   @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                   RedirectAttributes redirectAttributes) {
        try {
            facilityService.register(dto, imageFile);
            redirectAttributes.addFlashAttribute("message", "새 시설이 성공적으로 등록되었습니다.");
            redirectAttributes.addFlashAttribute("status", "success");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "등록 중 오류가 발생했습니다: " + e.getMessage());
            redirectAttributes.addFlashAttribute("status", "error");
        }
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
                                 @ModelAttribute FacilityRequestDto dto,
                                 @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                 @RequestParam(defaultValue = "false") boolean deleteImage,
                                 RedirectAttributes redirectAttributes) {
        try {
            facilityService.update(id, dto, imageFile, deleteImage);
            redirectAttributes.addFlashAttribute("message", "시설 정보가 수정되었습니다.");
            redirectAttributes.addFlashAttribute("status", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "수정 중 오류가 발생했습니다.");
            redirectAttributes.addFlashAttribute("status", "error");
        }
        return "redirect:/reservation/admin/list";
    }

    // 시설 삭제
    @PostMapping("/admin/facilities/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteFacility(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        // 1. 서비스 호출해서 DB 데이터 삭제
        facilityService.delete(id);
        redirectAttributes.addFlashAttribute("message", "시설이 삭제되었습니다.");
        redirectAttributes.addFlashAttribute("status", "success");
        // 2. 삭제 후 다시 관리 목록 페이지
        return "redirect:/reservation/admin/list";
    }


    // 캘린더 전용
    @GetMapping("/calendar")
    public String calendarPage() {
        return "reservation/calendar";
    }

}