package com.hometalk.onepass.parking.controller;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.auth.repository.HouseholdRepository;
import com.hometalk.onepass.parking.dto.response.ParkingHistoryResponse;
import com.hometalk.onepass.parking.repository.ParkingLogRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/parking")
public class ParkingLogController {

    private final ParkingLogRepository parkingLogRepository;
    private final HouseholdRepository householdRepository;

    @GetMapping("/logs")
    public String parkingLogsPage() {
        return "parking/parking-log";
    }

    @GetMapping("/logs/data")
    @ResponseBody
    public ResponseEntity<Page<ParkingHistoryResponse>> getParkingLogs(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long householdId = userDetails.getHouseholdId();
        householdRepository.findById(householdId)
                .orElseThrow(() -> new EntityNotFoundException("세대를 찾을 수 없습니다."));

        Pageable pageable = PageRequest.of(page, size, Sort.by("entryTime").descending());

        Page<ParkingHistoryResponse> logs = parkingLogRepository
                .findByHouseholdAndYearAndMonth(householdId, year, month, pageable)
                .map(ParkingHistoryResponse::new);

        return ResponseEntity.ok(logs);
    }

    @GetMapping("/guide")
    public String parkingGuide() {
        return "parking/parking-guide";
    }

    @GetMapping("/unregistered")
    public String unregisteredPage() {
        return "parking/visit-management";
    }

    @GetMapping("/exit")
    public String exitPage() {
        return "parking/exit";
    }
}