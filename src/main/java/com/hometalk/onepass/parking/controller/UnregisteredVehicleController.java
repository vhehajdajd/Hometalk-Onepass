package com.hometalk.onepass.parking.controller;

import com.hometalk.onepass.parking.dto.request.HouseholdMatchRequest;
import com.hometalk.onepass.parking.dto.response.UnregisteredVehicleResponse;
import com.hometalk.onepass.parking.service.UnregisteredVehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/parking")
@RequiredArgsConstructor
public class UnregisteredVehicleController {

    private final UnregisteredVehicleService unregisteredVehicleService;

    // GET /parking/unregistered/search?keyword=1234
    @GetMapping("/unregistered/search")
    public ResponseEntity<List<UnregisteredVehicleResponse>> search(
            @RequestParam String keyword) {

        if (keyword == null || keyword.strip().length() != 4) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(unregisteredVehicleService.search(keyword.strip()));
    }

    // POST /parking/unregistered/match
    @PostMapping("/unregistered/match")
    public ResponseEntity<Void> match(
            @RequestBody HouseholdMatchRequest request) {

        if (request.getEffectiveId() == null) {
            return ResponseEntity.badRequest().build();
        }

        Long householdId = 1L; // TODO: JWT 연동 후 추출
        unregisteredVehicleService.match(request.getEffectiveId(), householdId);
        return ResponseEntity.ok().build();
    }
}