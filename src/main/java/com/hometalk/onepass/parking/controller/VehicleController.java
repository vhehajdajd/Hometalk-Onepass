package com.hometalk.onepass.parking.controller;

import com.hometalk.onepass.parking.dto.request.VehicleRegisterRequest;
import com.hometalk.onepass.parking.dto.response.VehicleResponse;
import com.hometalk.onepass.parking.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/parking")
public class VehicleController {

    private final VehicleService vehicleService;

    // 세대 차량 목록 조회 페이지
    @GetMapping("/vehicle")
    public String vehicleList(Model model) {
        Long householdId = null; // TODO: JWT 연동 후 추출
        List<VehicleResponse> vehicles = vehicleService.getHouseholdVehicles(householdId);
        model.addAttribute("vehicles", vehicles);
        return "parking/vehicle-status";
    }

    // 차량 등록 페이지
    @GetMapping("/vehicle/register")
    public String vehicleRegisterPage(Model model) {
        return "parking/vehicle-register";
    }

    // 차량 등록 처리
    @PostMapping("/vehicle/register")
    public String vehicleRegister(
            @ModelAttribute VehicleRegisterRequest request,
            @RequestParam(value = "documents") List<MultipartFile> documents) {
        Long userId = null; // TODO: JWT 연동 후 추출
        vehicleService.register(userId, request, documents);
        return "redirect:/parking/vehicle";
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
            @RequestParam(value = "documents") List<MultipartFile> documents) {
        vehicleService.reapply(vehicleId, documents);
        return "redirect:/parking/vehicle";
    }
}