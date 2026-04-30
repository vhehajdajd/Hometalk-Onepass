package com.hometalk.onepass.reservation.service;

import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.auth.repository.UserRepository;
import com.hometalk.onepass.facility.entity.Facility;
import com.hometalk.onepass.facility.repository.FacilityRepository;
import com.hometalk.onepass.reservation.dto.ReservationRequestDto;
import com.hometalk.onepass.reservation.dto.ReservationResponseDto;
import com.hometalk.onepass.reservation.entity.Reservation;
import com.hometalk.onepass.reservation.entity.ReservationStatus;
import com.hometalk.onepass.reservation.entity.ReservationTime;
import com.hometalk.onepass.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final FacilityRepository facilityRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;

    /**
     * 시설 예약 등록 (DTO 기반 - String 날짜/시간 처리)
     */
    @Transactional
    public Long register(ReservationRequestDto dto) {
        // 1. 시설 확인 (이름으로 찾기)
        Facility facility = facilityRepository.findByName(dto.getFacilityName())
                .orElseThrow(() -> new RuntimeException("해당 시설을 찾을 수 없습니다."));

        // 2. 유저 확인 (현재는 DTO의 userId 사용, 추후 세션/Security에서 가져오도록 변경 가능)
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("해당 회원을 찾을 수 없습니다."));

        // 3. 문자열(String)을 LocalDateTime으로 변환
        // 포맷: "2026-05-09" + " " + "19:00" -> "2026-05-09 19:00"
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        java.time.LocalDateTime startDateTime = java.time.LocalDateTime.parse(dto.getReservationDate() + " " + dto.getStartTime(), formatter);
        java.time.LocalDateTime endDateTime = java.time.LocalDateTime.parse(dto.getReservationDate() + " " + dto.getEndTime(), formatter);

        // 4. 중복 예약 체크 (유저당 1건 제한 로직 유지 시)
        boolean existsMember = reservationRepository.existsByFacilityIdAndUserId(
                facility.getId(),
                user.getId());

        if (existsMember) {
            throw new RuntimeException("이미 이 시설에 대한 예약 내역이 존재합니다.");
        }

        // 5. 시간 중복 체크
        ReservationTime reservationTime = new ReservationTime(startDateTime, endDateTime);
        boolean existsTime = reservationRepository.existsByFacilityIdAndReservationTime(
                facility.getId(),
                reservationTime);

        if (existsTime) {
            throw new RuntimeException("해당 시간에 다른 예약자가 있습니다.");
        }

        // 6. 엔티티 생성 및 저장
        Reservation reservation = Reservation.builder()
                .facility(facility)
                .user(user)
                .reservationTime(reservationTime)
                .status(ReservationStatus.CONFIRMED)
                .build();

        return reservationRepository.save(reservation).getId();
    }

    /**
     * 특정 예약 조회 (엔티티 반환)
     */
    public Reservation findOne(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 예약을 찾을 수 없습니다."));
    }

    /**
     * 모든 예약 조회 (DTO 리스트 반환)
     */
    public List<ReservationResponseDto> findAll() {
        return reservationRepository.findAll().stream()
                .map(ReservationResponseDto::fromEntity)
                .toList();
    }

    /**
     * 예약 취소
     */
    @Transactional
    public void cancel(Long id) {
        Reservation reservation = findOne(id);
        reservation.cancel(); // Reservation 엔티티에 cancel() 메서드가 있어야 합니다.
    }

    /**
     * 관리자용: 모든 예약 내역을 최신순으로 상세 조회
     */
    public List<ReservationResponseDto> findAllWithDetails() {
        // Repository에서 작성한 최신순 조회 메서드를 호출합니다.
        return reservationRepository.findAllByOrderByIdDesc().stream()
                .map(ReservationResponseDto::fromEntity)
                .toList();
    }
}