package com.hometalk.onepass.reservation.controller;

import com.hometalk.onepass.facility.dto.FacilityRequestDto;
import com.hometalk.onepass.facility.service.FacilityService;
import com.hometalk.onepass.reservation.dto.ReservationRequestDto;
import com.hometalk.onepass.reservation.dto.ReservationResponseDto;
import com.hometalk.onepass.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final FacilityService facilityService;

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
        // 서비스에서 엔티티를 가져온 뒤 DTO로 변환해서 반환
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
       예약 취소
     */
    @PatchMapping("/{id}/cancel")
    public void cancel(@PathVariable("id") Long id) {
        reservationService.cancel(id);
    }

}