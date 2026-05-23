package com.hometalk.onepass.facility.controller;

import com.hometalk.onepass.facility.dto.FacilityRequestDto; // DTO 사용
import com.hometalk.onepass.facility.dto.FacilityResponseDto;
import com.hometalk.onepass.facility.entity.Facility;
import com.hometalk.onepass.facility.service.FacilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/facility")
public class FacilityController {

    private final FacilityService facilityService;

    /*
     * 시설 등록 (DTO 기반)
     */
    @PostMapping
    public Long register(@Valid @RequestBody FacilityRequestDto dto) { // Facility 대신 DTO를 받습니다.
        return facilityService.register(dto);
    }

    /*
     * 시설 전체 목록 조회
     */
    @GetMapping
    public List<FacilityResponseDto> findAll() {
        return facilityService.findAll();
    }

    /*
     * 시설 상세 조회
     */
    @GetMapping("/{id}")
    public FacilityResponseDto findOne(@PathVariable Long id) {
        return facilityService.findOne(id);
    }

    /*
     * 시설 정보 수정
     */
    @PatchMapping("/{id}")
    public void update(@PathVariable Long id,@Valid @RequestBody FacilityRequestDto dto) {
        facilityService.update(id, dto);
    }

    /*
     * 시설 삭제
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        facilityService.delete(id);
    }
}