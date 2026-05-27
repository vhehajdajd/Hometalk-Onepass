package com.hometalk.onepass.parking.controller;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.parking.dto.request.HouseholdMatchRequest;
import com.hometalk.onepass.parking.dto.response.UnregisteredVehicleResponse;
import com.hometalk.onepass.parking.service.UnregisteredVehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/parking")
@RequiredArgsConstructor
public class UnregisteredVehicleController {

    private final UnregisteredVehicleService unregisteredVehicleService;

    @GetMapping("/unregistered/search")
    public ResponseEntity<List<UnregisteredVehicleResponse>> search(
            @RequestParam String keyword) {

        if (keyword == null || keyword.strip().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(unregisteredVehicleService.search(keyword.strip()));
    }

    @PostMapping("/unregistered/match")
    public ResponseEntity<Void> match(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody HouseholdMatchRequest request) {

        if (request.getEffectiveId() == null) {
            return ResponseEntity.badRequest().build();
        }

        Long householdId = userDetails.getHouseholdId();
        unregisteredVehicleService.match(request.getEffectiveId(), householdId);
        return ResponseEntity.ok().build();
    }
}