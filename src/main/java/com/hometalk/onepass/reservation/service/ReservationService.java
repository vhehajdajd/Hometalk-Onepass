package com.hometalk.onepass.reservation.service;

import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.auth.repository.UserRepository;
import com.hometalk.onepass.facility.entity.Facility;
import com.hometalk.onepass.facility.repository.FacilityRepository;
import com.hometalk.onepass.reservation.dto.ReservationCalendarDto;
import com.hometalk.onepass.reservation.dto.ReservationRequestDto;
import com.hometalk.onepass.reservation.dto.ReservationResponseDto;
import com.hometalk.onepass.reservation.entity.Reservation;
import com.hometalk.onepass.reservation.entity.ReservationStatus;
import com.hometalk.onepass.reservation.entity.ReservationTime;
import com.hometalk.onepass.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final FacilityRepository facilityRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;

    /*
     * 시설 예약 등록 (DTO 기반)
     */
    @Transactional
    public Long register(ReservationRequestDto dto) {
        // 1. 시설 확인
        Facility facility = facilityRepository.findById(dto.getFacilityId())
                .orElseThrow(() -> new RuntimeException("해당 시설을 찾을 수 없습니다."));

        // 2. 유저 확인
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("해당 회원을 찾을 수 없습니다."));

        // 3. 중복 예약 체크
        boolean existsMember = reservationRepository.existsByFacilityIdAndUserId(
                facility.getId(),
                user.getId());

        if (existsMember) {
            throw new RuntimeException("이미 이 시설에 대한 예약 내역이 존재합니다.");
        }

        // 4. 시간 중복 체크
        boolean existsTime = reservationRepository.existsByFacilityIdAndReservationTime(
                facility.getId(),
                new ReservationTime(dto.getStartTime(), dto.getEndTime()));

        if (existsTime) {
            throw new RuntimeException("해당 시간에 다른 예약자가 있습니다.");
        }

        // 5. 엔티티 생성 및 저장
        Reservation reservation = Reservation.builder()
                .facility(facility)
                .user(user)
                .reservationTime(new ReservationTime(dto.getStartTime(), dto.getEndTime()))
                .status(ReservationStatus.PENDING)
                .build();

        return reservationRepository.save(reservation).getId();
    }

    /*
     * 특정 예약 조회 (엔티티 반환)
     * 컨트롤러에서 .fromEntity()로 변환해서 쓸 수 있게 엔티티를 던져줍니다.
     */
    public Reservation findOne(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 예약을 찾을 수 없습니다."));
    }

    /*
     * 모든 예약 조회 (DTO 리스트 반환)
     */
    public List<ReservationResponseDto> findAll() {
        return reservationRepository.findAll().stream()
                .map(ReservationResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /*
     * 예약 취소
     */
    @Transactional
    public void cancel(Long id) {
        Reservation reservation = findOne(id);
        reservation.cancel();
    }

    /* * 관리자용: 모든 예약 내역을 최신순으로 조회
     * 에러 해결 부분
     */
    public List<ReservationResponseDto> findAllWithDetails() {
        return reservationRepository.findAllByOrderByIdDesc().stream()
                .map(ReservationResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    // 캘린더 전용
    public List<ReservationCalendarDto> getCalendar(int year, int month) {
        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime end = start.plusMonths(1).minusSeconds(1);

        return reservationRepository.findByMonthRange(start, end)
                .stream()
                .map(ReservationCalendarDto::from)
                .collect(Collectors.toList());
    }

    // 내 예약 현황
    public List<ReservationResponseDto> findByUserId(Long userId) {
        return reservationRepository.findByUserIdOrderByIdDesc(userId)
                .stream().map(ReservationResponseDto::fromEntity)
                .collect(Collectors.toList());
    }
}