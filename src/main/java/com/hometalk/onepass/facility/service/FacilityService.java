package com.hometalk.onepass.facility.service;

import com.hometalk.onepass.facility.dto.FacilityRequestDto;
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

    /**
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

    /**
     * 시설 삭제: 관리자 페이지에서 호출됩니다.
     */
    @Transactional
    public void delete(Long id) {
        Facility facility = findOne(id);
        facilityRepository.delete(facility);
    }

    public List<Facility> findAll() {
        return facilityRepository.findAll();
    }

    public Facility findOne(Long id) {
        return facilityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 시설을 찾을 수 없습니다."));
    }

    /**
     * 시설 정보 수정
     */
    @Transactional
    public void update(Long id, FacilityRequestDto dto) {
        // 1. 기존 시설 조회
        Facility facility = findOne(id);

        // 2. 수정할 시간 데이터 생성 (String -> LocalTime 변환)
        OperationTime opTime = OperationTime.builder()
                .openTime(LocalTime.parse(dto.getOpenTime()))
                .closeTime(LocalTime.parse(dto.getCloseTime()))
                .build();

        // 3. 엔티티의 비즈니스 로직 호출 (변경 감지 활용)
        facility.updateInfo(
                dto.getName(),
                dto.getLocation(),
                dto.getIconType(),
                dto.getImagePath(),
                opTime
        );

        // @Transactional 안에서 엔티티를 수정하면
        // 따로 save를 호출하지 않아도 메서드 종료 시점에 DB에 반영됩니다.
    }
}