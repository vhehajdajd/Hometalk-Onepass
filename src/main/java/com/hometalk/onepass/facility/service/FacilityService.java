package com.hometalk.onepass.facility.service;

import com.hometalk.onepass.community.service.FileService;
import com.hometalk.onepass.facility.dto.FacilityRequestDto;
import com.hometalk.onepass.facility.dto.FacilityResponseDto;
import com.hometalk.onepass.facility.entity.BookingStatus;
import com.hometalk.onepass.facility.entity.Facility;
import com.hometalk.onepass.facility.entity.OperationTime;
import com.hometalk.onepass.facility.repository.FacilityRepository;
import com.hometalk.onepass.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacilityService {

    private final FacilityRepository facilityRepository;
    private final ReservationRepository reservationRepository;
    private final FileService fileService;

    @Value("${file.upload.path}")
    private String uploadPath;

    /* 시설 조회 */
    private Facility getFacility(Long id) {
        return facilityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 시설을 찾을 수 없습니다."));
    }

    /*
     * 시설 등록
     */
    @Transactional
    public Long register(FacilityRequestDto dto, MultipartFile file) throws IOException {
        String imagePath = null;
        if (file != null && !file.isEmpty()) {
            String storeFileName = fileService.storeFile(file);
            imagePath = "/uploads/" + storeFileName;
        }

        OperationTime opTime = OperationTime.builder()
                .openTime(LocalTime.parse(dto.getOpenTime()))
                .closeTime(LocalTime.parse(dto.getCloseTime()))
                .build();

        Facility facility = Facility.builder()
                .name(dto.getName())
                .location(dto.getLocation())
                .iconType(dto.getIconType())
                .imagePath(imagePath)
                .maxCapacity(dto.getMaxCapacity())
                .operationTime(opTime)
                .maxReservationTime(dto.getMaxReservationTime())
                .build();

        return facilityRepository.save(facility).getId();
    }

    /*
     * 시설 삭제: 관리자 페이지에서 호출됩니다.
     */
    @Transactional
    public void delete(Long id) {
        Facility facility = getFacility(id);
        if (facility.getImagePath() != null) {
            String realPath = uploadPath + facility.getImagePath().replace("/uploads", "");
            File diskFile = new File(realPath);
            if (diskFile.exists()) {
                diskFile.delete();
            }
        }
        facilityRepository.delete(facility);
    }

    public List<FacilityResponseDto> findAll() {
        return facilityRepository.findAll().stream()
                .map(FacilityResponseDto::from)
                .toList();
    }

    public FacilityResponseDto findOne(Long id) {
        Facility facility = getFacility(id);
        return FacilityResponseDto.from(facility);
    }

    /*
     * 시설 정보 수정
     */
    @Transactional
    public void update(Long id, FacilityRequestDto dto, MultipartFile file, boolean deleteImage) throws IOException {
        Facility facility = getFacility(id);
        String imagePath = facility.getImagePath();
        if (deleteImage && facility.getImagePath() != null) {
            String oldFilePath = uploadPath + facility.getImagePath().replace("/uploads", "");
            File oldFile = new File(oldFilePath);
            if (oldFile.exists()) {
                oldFile.delete();
            }
            imagePath = null;
        }

        if (file != null && !file.isEmpty()) {
            if (facility.getImagePath() != null) {
                String oldFilePath = uploadPath + facility.getImagePath().replace("/uploads", "");
                File oldFile = new File(oldFilePath);
                if (oldFile.exists()) {
                    oldFile.delete();
                }
            }
            String storeFileName = fileService.storeFile(file);
            imagePath = "/uploads/" + storeFileName;
        }

        OperationTime opTime = OperationTime.builder()
                .openTime(LocalTime.parse(dto.getOpenTime()))
                .closeTime(LocalTime.parse(dto.getCloseTime()))
                .build();

        // 최종 반영 (새 파일 주소 or 유지된 기존 주소)
        facility.updateInfo(dto.getName(),
                dto.getLocation(),
                dto.getIconType(),
                imagePath,
                dto.getMaxCapacity(),
                opTime,
                dto.getMaxReservationTime());
    }

    public List<FacilityResponseDto> getFacilitiesWithStatus(Long userId) {
        List<Facility> facilities = facilityRepository.findAll();
        LocalTime now = LocalTime.now();
        LocalDateTime nowDateTime = LocalDateTime.now();

        return facilities.stream().map(facility -> {
            boolean hasActiveBooking = reservationRepository.existsActiveReservation(
                    userId,
                    facility.getId(),
                    nowDateTime
            );

            if (hasActiveBooking) {
                return FacilityResponseDto.from(facility, BookingStatus.ALREADY_BOOKED);
            }

            if (facility.getOperationTime() != null && facility.getOperationTime().getCloseTime() != null) {
                LocalTime closeTime = facility.getOperationTime().getCloseTime();

                if (now.isAfter(closeTime)) {
                    return FacilityResponseDto.from(facility, BookingStatus.CLOSED);
                }
            }

            return FacilityResponseDto.from(facility, BookingStatus.AVAILABLE);
        }).collect(Collectors.toList());
    }
}