package com.hometalk.onepass.reservation.controller;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.reservation.dto.ReservationCalendarDto;
import com.hometalk.onepass.reservation.dto.ReservationRequestDto;
import com.hometalk.onepass.reservation.dto.ReservationResponseDto;
import com.hometalk.onepass.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    /*
       시설 예약 등록
     */
    @PostMapping
    public Long register(@RequestBody ReservationRequestDto dto,
                         Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new RuntimeException("로그인 정보가 유효하지 않습니다. 다시 로그인해주세요.");
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return reservationService.register(dto, userDetails.getUserId());
    }

    /*
       특정 예약 상세 조회
     */
    @GetMapping("/{id}")
    public ReservationResponseDto findOne(@PathVariable Long id,
                                          Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new RuntimeException("인증 정보가 없습니다.");
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return reservationService.findOne(id, userDetails.getUserId(), userDetails.getRole());
    }

    /*
       모든 예약 조회
     */
    @GetMapping
    public List<ReservationResponseDto> list() {
        return reservationService.findAll();
    }

    /*
        예약 승인 [관리자]
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> approveReservation(@PathVariable Long id) {
        reservationService.approve(id); // 서비스의 approve 메서드 호출
        return ResponseEntity.ok().build();
    }

    /*
       예약 취소 [사용자/관리자 공용]
     */
    @PatchMapping("/{id}/cancel")
    public void cancel(@PathVariable("id") Long id,
                       @RequestBody(required = false) Map<String, String> body,
                       Authentication authentication) {

        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new RuntimeException("인증 정보가 없습니다.");
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        String reason = null;

        if (body != null) {
            reason = body.get("reason");
        }

        reservationService.cancel(
                id,
                userDetails.getUserId(),
                userDetails.getRole(),
                reason
        );
    }


    // 캘린더 전용
    @GetMapping("/calendar")
    public List<ReservationCalendarDto> calendar(
            @RequestParam int year,
            @RequestParam int month
    ) {
        return reservationService.getCalendar(year, month);
    }
}