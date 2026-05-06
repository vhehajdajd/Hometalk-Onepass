package com.hometalk.onepass.reservation.controller;

import com.hometalk.onepass.facility.service.FacilityService;
import com.hometalk.onepass.reservation.dto.ReservationCalendarDto;
import com.hometalk.onepass.reservation.dto.ReservationRequestDto;
import com.hometalk.onepass.reservation.dto.ReservationResponseDto;
import com.hometalk.onepass.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    /*
       시설 예약 등록
     */
    @PostMapping
    public Long register(@RequestBody ReservationRequestDto dto) {
        return reservationService.register(dto);
    }

    /*
       특정 예약 상세 조회
     */
    @GetMapping("/{id}")
    public ReservationResponseDto findOne(@PathVariable Long id) {
        return ReservationResponseDto.fromEntity(reservationService.findOne(id));
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
    public ResponseEntity<Void> approveReservation(@PathVariable Long id) {
        reservationService.approve(id); // 서비스의 approve 메서드 호출
        return ResponseEntity.ok().build();
    }

    /*
       예약 취소 [사용자/관리자 공용]
     */
    @PatchMapping("/{id}/cancel")
    public void cancel(@PathVariable("id") Long id) {
        reservationService.cancel(id);
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