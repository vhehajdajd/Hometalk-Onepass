package com.hometalk.onepass.parking.controller;

import com.hometalk.onepass.auth.repository.HouseholdRepository;
import com.hometalk.onepass.parking.dto.response.ParkingHistoryResponse;
import com.hometalk.onepass.parking.repository.ParkingLogRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/parking")
public class ParkingLogController {

    private final ParkingLogRepository parkingLogRepository;
    private final HouseholdRepository householdRepository;

    // 월별 주차 기록 조회 페이지
    @GetMapping("/logs")
    public String parkingLogsPage() {
        return "parking/parking-log";
    }

    // 월별 주차 기록 조회 API
    @GetMapping("/logs/data")
    @ResponseBody
    public ResponseEntity<List<ParkingHistoryResponse>> getParkingLogs(
            @RequestParam int year,
            @RequestParam int month) {

        Long householdId = 1L;
        householdRepository.findById(householdId)
                .orElseThrow(() -> new EntityNotFoundException("세대를 찾을 수 없습니다."));

        List<ParkingHistoryResponse> logs = parkingLogRepository
                .findByHouseholdAndYearAndMonth(householdId, year, month)
                .stream()
                .map(ParkingHistoryResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(logs);
    }

    // 주차 이용 안내 페이지
    @GetMapping("/guide")
    public String parkingGuide() {
        return "parking/parking-guide";
    }

    // 방문 차량 관리 페이지
    @GetMapping("/unregistered")
    public String unregisteredPage() {
        return "parking/visit-management";
    }

    // 출차
    @GetMapping("/exit")
    public String exitPage() {
        return "parking/exit";
    }
}