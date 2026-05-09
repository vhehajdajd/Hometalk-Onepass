package com.hometalk.onepass.reservation.controller;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.reservation.dto.ReservationResponseDto;
import com.hometalk.onepass.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReservationDashboardApiController {

    private final ReservationService reservationService;

    @GetMapping("/api/reservation/my-recent")
    public ResponseEntity<List<ReservationResponseDto>> myRecent(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ResponseEntity.ok(
                reservationService.findMyRecent(user.getUserId())
        );
    }

    @GetMapping("/api/admin/reservation/recent")
    public ResponseEntity<List<ReservationResponseDto>> adminRecent() {
        return ResponseEntity.ok(
                reservationService.findAdminRecent()
        );
    }
}