package com.hometalk.onepass.parking.controller;

import com.hometalk.onepass.parking.dto.response.AdminParkingLogResponse;
import com.hometalk.onepass.parking.repository.ParkingLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminParkingController {

    private final ParkingLogRepository parkingLogRepository;

    // 관리자 전체 주차 기록 페이지
    @GetMapping("/parking/logs")
    public String adminParkingLogsPage() {
        return "parking/admin-parking-log";
    }

    // 관리자 전체 주차 기록 조회 API
    @GetMapping("/parking/logs/data")
    @ResponseBody
    public ResponseEntity<Page<AdminParkingLogResponse>> getAdminParkingLogs(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(defaultValue = "ALL") String type,
            @RequestParam(defaultValue = "") String household,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("entryTime").descending());

        Page<AdminParkingLogResponse> logs = parkingLogRepository
                .findAllByFilter(year, month, type, household, pageable)
                .map(AdminParkingLogResponse::new);

        return ResponseEntity.ok(logs);
    }
}