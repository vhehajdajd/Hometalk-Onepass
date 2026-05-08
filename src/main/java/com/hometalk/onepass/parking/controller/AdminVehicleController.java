package com.hometalk.onepass.parking.controller;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.parking.dto.request.VehicleApprovalRequest;
import com.hometalk.onepass.parking.dto.response.VehicleApprovalResponse;
import com.hometalk.onepass.parking.entity.Vehicle;
import com.hometalk.onepass.parking.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminVehicleController {

    private final VehicleService vehicleService;

    // 관리자 차량 승인 목록 페이지
    @GetMapping("/vehicle/approval")
    public String approvalPage() {
        return "parking/admin-vehicle-approval";
    }

    // 관리자 차량 목록 조회 (JSON)
    @GetMapping("/vehicle/approval/list")
    @ResponseBody
    public ResponseEntity<Page<VehicleApprovalResponse>> getApprovalList(
            @RequestParam Vehicle.VehicleStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(vehicleService.getApprovalList(status, page, size));
    }

    // 관리자 차량 승인 처리
    @PostMapping("/vehicle/approval/approve")
    @ResponseBody
    public ResponseEntity<Void> approve(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody VehicleApprovalRequest request) {
        Long userId = userDetails.getUserId();
        vehicleService.approve(userId, request.getApprovalId());
        return ResponseEntity.ok().build();
    }

    // 관리자 차량 반려 처리
    @PostMapping("/vehicle/approval/reject")
    @ResponseBody
    public ResponseEntity<Void> reject(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody VehicleApprovalRequest request) {
        Long userId = userDetails.getUserId();
        vehicleService.reject(userId, request.getApprovalId(), request.getRejectReason());
        return ResponseEntity.ok().build();
    }

    // 관리자 차량 삭제
    @PostMapping("/vehicle/approval/delete/{vehicleId}")
    @ResponseBody
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long vehicleId) {
        vehicleService.adminDelete(vehicleId);
        return ResponseEntity.ok().build();
    }
}