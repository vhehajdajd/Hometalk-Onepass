package com.hometalk.onepass.reservation.controller;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.reservation.dto.ReservationCalendarDto;
import com.hometalk.onepass.reservation.dto.ReservationRequestDto;
import com.hometalk.onepass.reservation.dto.ReservationResponseDto;
import com.hometalk.onepass.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    public Page<ReservationResponseDto> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return reservationService.findAll(page, size);
    }

    /*
        특정 시설의 특정 날짜 시간대별 예약 현황(인원수) 조회
     */
    @GetMapping("/capacity")
    public ResponseEntity<Map<Integer, Integer>> getCapacity(
            @RequestParam Long facilityId,
            @RequestParam String date) {
        java.time.LocalDate localDate = java.time.LocalDate.parse(date);
        Map<Integer, Integer> capacityMap = reservationService.getHourlyCapacity(facilityId, localDate);

        return ResponseEntity.ok(capacityMap);
    }

    @GetMapping("/user-booked-times")
    public ResponseEntity<List<Integer>> getUserBookedTimes(@RequestParam String date,
                                                            Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return ResponseEntity.status(401).build();
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        java.time.LocalDate localDate = java.time.LocalDate.parse(date);
        List<Integer> bookedHours = reservationService.getUserReservedHours(userDetails.getUserId(), localDate);
        return ResponseEntity.ok(bookedHours);
    }

    /*
       특정 시설에 대해 현재 '이용 종료/취소됨' 상태가 아닌 예약이 있는지 확인
     */
    @GetMapping("/check-active/{facilityId}")
    public ResponseEntity<?> checkActiveReservation(@PathVariable Long facilityId,
                                                    Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return ResponseEntity.status(401).body("인증 정보가 없습니다.");
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        boolean hasActive = reservationService.hasActiveReservation(userDetails.getUserId(), facilityId);
        if (hasActive) {
            return ResponseEntity.ok(Map.of(
                    "hasActive", true,
                    "message", "해당 시설에 이미 예약 또는 이용 중인 내역이 존재합니다. 이용 종료 후 다시 예약해주세요."
            ));
        }
        return ResponseEntity.ok(Map.of("hasActive", false));
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
            - 사용자: 직접 취소(CANCELED)
            - 관리자: 사유 입력 후 반려(REJECTED)
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

    /*
        이용 종료
     */
    @PatchMapping("/{id}/finish")
    public ResponseEntity<?> finish(@PathVariable Long id,
                                    Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new RuntimeException("인증 정보가 없습니다.");
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        try {
            reservationService.finishUsage(id, userDetails.getUserId());
            return ResponseEntity.ok().body("이용 종료 처리가 완료되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    // 캘린더 전용
    @GetMapping("/calendar")
    public List<ReservationCalendarDto> calendar(
            @RequestParam Long facilityId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return reservationService.getCalendar(facilityId, year, month);
    }
}