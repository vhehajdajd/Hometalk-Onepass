package com.hometalk.onepass.facility.service;

import com.hometalk.onepass.facility.dto.FacilityRequestDto;
import com.hometalk.onepass.facility.dto.FacilityResponseDto;
import com.hometalk.onepass.facility.entity.Facility;
import com.hometalk.onepass.facility.entity.OperationTime;
import com.hometalk.onepass.facility.repository.FacilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacilityService {

    private final FacilityRepository facilityRepository;

    /* 시설 조회 */
    private Facility getFacility(Long id) {
        return facilityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 시설을 찾을 수 없습니다."));
    }

    /*
     * 시설 등록: String 시간을 LocalTime으로 파싱하여 저장합니다.
     */
    @Transactional
    public Long register(FacilityRequestDto dto) {
        OperationTime opTime = OperationTime.builder()
                .openTime(LocalTime.parse(dto.getOpenTime()))
                .closeTime(LocalTime.parse(dto.getCloseTime()))
                .build();

        Facility facility = Facility.builder()
                .name(dto.getName())
                .location(dto.getLocation())
                .iconType(dto.getIconType())
                .imagePath(dto.getImagePath())
                .operationTime(opTime)
                .build();

        return facilityRepository.save(facility).getId();
    }

    /*
     * 시설 삭제: 관리자 페이지에서 호출됩니다.
     */
    @Transactional
    public void delete(Long id) {
        Facility facility = getFacility(id);
        facilityRepository.delete(facility);
    }

    public List<FacilityResponseDto> findAll() {
        return facilityRepository.findAll().stream()
                .map(f -> FacilityResponseDto.builder()
                        .id(f.getId())
                        .name(f.getName())
                        .location(f.getLocation())
                        .iconType(f.getIconType())
                        .openTime(f.getOperationTime().getFormattedOpenTime())
                        .closeTime(f.getOperationTime().getFormattedCloseTime())
                        .build())
                .toList();
    }

    public FacilityResponseDto findOne(Long id) {
        Facility facility = getFacility(id);

        return FacilityResponseDto.builder()
                .id(facility.getId())
                .name(facility.getName())
                .location(facility.getLocation())
                .iconType(facility.getIconType())
                .openTime(facility.getOperationTime().getFormattedOpenTime())
                .closeTime(facility.getOperationTime().getFormattedCloseTime())
                .build();
    }

    /*
     * 시설 정보 수정
     */
    @Transactional
    public void update(Long id, FacilityRequestDto dto) {

        Facility facility = getFacility(id);

        OperationTime opTime = OperationTime.builder()
                .openTime(LocalTime.parse(dto.getOpenTime()))
                .closeTime(LocalTime.parse(dto.getCloseTime()))
                .build();

        facility.updateInfo(
                dto.getName(),
                dto.getLocation(),
                dto.getIconType(),
                dto.getImagePath(),
                opTime
        );
    }
}