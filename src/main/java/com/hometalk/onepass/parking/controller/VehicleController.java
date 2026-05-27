package com.hometalk.onepass.parking.controller;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.parking.dto.request.VehicleRegisterRequest;
import com.hometalk.onepass.parking.dto.response.VehicleResponse;
import com.hometalk.onepass.parking.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/parking")
public class VehicleController {

    private final VehicleService vehicleService;

    // 세대 차량 목록 조회 페이지
    @GetMapping("/vehicle")
    public String vehicleList(@AuthenticationPrincipal CustomUserDetails userDetails,
                              Model model) {
        Long householdId = userDetails.getHouseholdId();
        List<VehicleResponse> vehicles = vehicleService.getHouseholdVehicles(householdId);
        model.addAttribute("vehicles", vehicles);
        return "parking/vehicle-status";
    }

    // 세대 차량 목록 조회 (JSON - 대시보드용)
    @GetMapping("/vehicle/list")
    @ResponseBody
    public ResponseEntity<List<VehicleResponse>> getVehicleList(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long householdId = userDetails.getHouseholdId();
        return ResponseEntity.ok(vehicleService.getHouseholdVehicles(householdId));
    }

    // 차량 등록 페이지
    @GetMapping("/vehicle/register")
    public String vehicleRegisterPage(Model model) {
        return "parking/vehicle-register";
    }

    // 차량 등록 처리
    @PostMapping("/vehicle/register")
    public String vehicleRegister(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ModelAttribute VehicleRegisterRequest request,
            @RequestParam(value = "documents") List<MultipartFile> documents,
            Model model) {
        try {
            Long userId = userDetails.getUserId();
            vehicleService.register(userId, request, documents);
            return "redirect:/parking/vehicle";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "parking/vehicle-register";
        }
    }

    // 반려 사유 조회 (JSON)
    @GetMapping("/vehicle/reject-reason/{vehicleId}")
    @ResponseBody
    public ResponseEntity<String> getRejectReason(@PathVariable Long vehicleId) {
        String rejectReason = vehicleService.getRejectReason(vehicleId);
        return ResponseEntity.ok(rejectReason != null ? rejectReason : "");
    }

    // 차량 재신청 페이지
    @GetMapping("/vehicle/reapply/{vehicleId}")
    public String vehicleReapplyPage(@PathVariable Long vehicleId, Model model) {
        VehicleResponse vehicle = vehicleService.getVehicle(vehicleId);
        String rejectReason = vehicleService.getRejectReason(vehicleId);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("rejectReason", rejectReason);
        return "parking/vehicle-reapply";
    }

    // 차량 재신청 처리
    @PostMapping("/vehicle/reapply/{vehicleId}")
    public String vehicleReapply(
            @PathVariable Long vehicleId,
            @RequestParam(value = "documents") List<MultipartFile> documents,
            Model model) {
        try {
            vehicleService.reapply(vehicleId, documents);
            return "redirect:/parking/vehicle";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "parking/vehicle-reapply";
        }
    }

    // 차량 삭제
    // ✅ 보안 수정: @AuthenticationPrincipal 추가 → Service에서 내 세대 차량인지 검증
    // 다른 세대가 URL에 vehicleId를 직접 입력해 삭제하는 것을 방지
    @PostMapping("/vehicle/delete/{vehicleId}")
    @ResponseBody
    public ResponseEntity<Void> deleteVehicle(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long vehicleId) {
        try {
            vehicleService.delete(vehicleId, userDetails.getHouseholdId());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 차량 번호 중복 체크 (실시간 검증용 AJAX)
    @GetMapping("/vehicle/check")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> checkVehicleNumber(
            @RequestParam String vehicleNumber) {
        String cleaned = vehicleNumber.replace(" ", "");
        boolean exists = vehicleService.existsByVehicleNumber(cleaned);
        return ResponseEntity.ok(Map.of("exists", exists));
    }
}