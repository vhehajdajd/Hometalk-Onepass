package com.hometalk.onepass.parking.controller;

import com.hometalk.onepass.parking.dto.request.ExitRequest;
import com.hometalk.onepass.parking.dto.response.ParkingLogResponse;
import com.hometalk.onepass.parking.service.StaffExitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/staff/exit")
@RequiredArgsConstructor
public class StaffExitController {

    private final StaffExitService staffExitService;

    // GET /staff/exit/search?keyword=1234
    @GetMapping("/search")
    public ResponseEntity<List<ParkingLogResponse>> searchParked(
            @RequestParam String keyword) {

        if (keyword == null || keyword.strip().length() != 4) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(staffExitService.search(keyword.strip()));
    }

    // GET /staff/exit/list/visit
    @GetMapping("/list/visit")
    public ResponseEntity<List<ParkingLogResponse>> getParkedVisitList() {
        return ResponseEntity.ok(staffExitService.getParkedVisitList());
    }

    // GET /staff/exit/list/resident
    @GetMapping("/list/resident")
    public ResponseEntity<List<ParkingLogResponse>> getParkedResidentList() {
        return ResponseEntity.ok(staffExitService.getParkedResidentList());
    }

    // POST /staff/exit/process
    @PostMapping("/process")
    public ResponseEntity<Void> processExit(@RequestBody ExitRequest request) {
        if (request.getParkingId() == null) {
            return ResponseEntity.badRequest().build();
        }
        staffExitService.processExit(request.getParkingId());
        return ResponseEntity.ok().build();
    }

    // POST /staff/exit/force
    @PostMapping("/force")
    public ResponseEntity<Void> forceExit(@RequestBody ExitRequest request) {
        if (request.getParkingId() == null) {
            return ResponseEntity.badRequest().build();
        }
        staffExitService.forceExit(request.getParkingId());
        return ResponseEntity.ok().build();
    }

    // POST /staff/exit/notify
    @PostMapping("/notify")
    public ResponseEntity<Void> sendNotification(@RequestBody ExitRequest request) {
        // TODO: 알림 담당자 코드 연동 후 구현
        return ResponseEntity.ok().build();
    }
}


