package com.hometalk.onepass.facility.controller;

import com.hometalk.onepass.community.service.FileService;
import com.hometalk.onepass.facility.dto.FacilityRequestDto; // DTO 사용
import com.hometalk.onepass.facility.dto.FacilityResponseDto;
import com.hometalk.onepass.facility.entity.Facility;
import com.hometalk.onepass.facility.service.FacilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/facility")
public class FacilityController {

    private final FacilityService facilityService;

    /*
     * 시설 등록
     */
    @PostMapping
    public ResponseEntity<?> register(@Valid @ModelAttribute FacilityRequestDto dto,
                                      @RequestParam(value = "facilityImage", required = false) MultipartFile file) {
        try {
            Long registeredId = facilityService.register(dto, file);
            return ResponseEntity.ok(registeredId);
        } catch (IOException e) {
            e.printStackTrace(); // 콘솔에 에러 로그 출력
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("시설 등록 중 파일 업로드에 실패했습니다.");
        }
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
    public ResponseEntity<String> update(@PathVariable Long id,
                                         @Valid @ModelAttribute FacilityRequestDto dto,
                                         @RequestParam(value = "facilityImage", required = false) MultipartFile file,
                                         @RequestParam(defaultValue = "false") boolean deleteImage) {
        try {
            facilityService.update(id, dto, file, deleteImage);
            return ResponseEntity.ok("시설 정보가 성공적으로 수정되었습니다.");
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("시설 수정 중 파일 업로드에 실패했습니다.");
        }
    }

    /*
     * 시설 삭제
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        facilityService.delete(id);
    }

    /*
      실시간 예약 상태
     */
    @GetMapping("/with-status")
    public List<FacilityResponseDto> findAllWithStatus(@RequestParam("userId") Long userId) {
        return facilityService.getFacilitiesWithStatus(userId);
    }

}