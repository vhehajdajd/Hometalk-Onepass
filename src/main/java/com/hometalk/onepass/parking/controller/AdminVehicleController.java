package com.hometalk.onepass.parking.controller;

import com.hometalk.onepass.parking.dto.request.VehicleApprovalRequest;
import com.hometalk.onepass.parking.dto.response.VehicleApprovalResponse;
import com.hometalk.onepass.parking.entity.Vehicle;
import com.hometalk.onepass.parking.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<List<VehicleApprovalResponse>> getApprovalList(
            @RequestParam Vehicle.VehicleStatus status) {
        return ResponseEntity.ok(vehicleService.getApprovalList(status));
    }

    // 관리자 차량 승인 처리
    @PostMapping("/vehicle/approval/approve")
    @ResponseBody
    public ResponseEntity<Void> approve(@RequestBody VehicleApprovalRequest request) {
        Long userId = null; // TODO: JWT 연동 후 추출
        vehicleService.approve(userId, request.getApprovalId());
        return ResponseEntity.ok().build();
    }

    // 관리자 차량 반려 처리
    @PostMapping("/vehicle/approval/reject")
    @ResponseBody
    public ResponseEntity<Void> reject(@RequestBody VehicleApprovalRequest request) {
        Long userId = null; // TODO: JWT 연동 후 추출
        vehicleService.reject(userId, request.getApprovalId(), request.getRejectReason());
        return ResponseEntity.ok().build();
    }
}