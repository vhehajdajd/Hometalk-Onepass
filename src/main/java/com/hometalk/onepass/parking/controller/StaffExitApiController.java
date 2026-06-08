package com.hometalk.onepass.parking.controller;

import com.hometalk.onepass.parking.dto.request.ExitRequest;
import com.hometalk.onepass.parking.dto.response.ParkingLogResponse;
import com.hometalk.onepass.parking.service.StaffExitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/exit")
@RequiredArgsConstructor
public class StaffExitApiController {

    private final StaffExitService staffExitService;

    @GetMapping("/search")
    public ResponseEntity<List<ParkingLogResponse>> searchParked(
            @RequestParam String keyword) {
        if (keyword == null || keyword.strip().length() != 4) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(staffExitService.search(keyword.strip()));
    }

    @GetMapping("/list/visit")
    public ResponseEntity<List<ParkingLogResponse>> getParkedVisitList() {
        return ResponseEntity.ok(staffExitService.getParkedVisitList());
    }

    @GetMapping("/list/resident")
    public ResponseEntity<List<ParkingLogResponse>> getParkedResidentList() {
        return ResponseEntity.ok(staffExitService.getParkedResidentList());
    }

    @GetMapping("/list/recent")
    public ResponseEntity<List<ParkingLogResponse>> getRecentExitList() {
        return ResponseEntity.ok(staffExitService.getRecentExitList());
    }

    @PostMapping("/process")
    public ResponseEntity<Void> processExit(@RequestBody ExitRequest request) {
        if (request.getParkingId() == null) {
            return ResponseEntity.badRequest().build();
        }
        staffExitService.processExit(request.getParkingId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/force")
    public ResponseEntity<Void> forceExit(@RequestBody ExitRequest request) {
        if (request.getParkingId() == null) {
            return ResponseEntity.badRequest().build();
        }
        staffExitService.forceExit(request.getParkingId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/notify")
    public ResponseEntity<Void> sendNotification(@RequestBody ExitRequest request) {
        if (request.getParkingId() == null) {
            return ResponseEntity.badRequest().build();
        }
        staffExitService.sendTicketShortageNotification(request.getParkingId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cancel")
    public ResponseEntity<Void> cancelExit(@RequestBody ExitRequest request) {
        if (request.getParkingId() == null) {
            return ResponseEntity.badRequest().build();
        }
        staffExitService.cancelExit(request.getParkingId());
        return ResponseEntity.ok().build();
    }
}