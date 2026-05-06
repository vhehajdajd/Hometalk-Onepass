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

import java.time.LocalDate;
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
       시설 예약 등록
     */
    @Transactional
    public Long register(ReservationRequestDto dto) {
        // 1. 시설 및 유저 확인
        Facility facility = facilityRepository.findById(dto.getFacilityId())
                .orElseThrow(() -> new RuntimeException("해당 시설을 찾을 수 없습니다."));
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("해당 회원을 찾을 수 없습니다."));

        LocalDateTime start = dto.getStartTime();
        LocalDateTime end = dto.getEndTime();
        LocalDateTime now = LocalDateTime.now();
        LocalDate targetDate = start.toLocalDate();

        // --- 비즈니스 로직 검증 ---

        // [로직 1] 과거 시간 예약 불가
        if (start.isBefore(now)) {
            throw new RuntimeException("현재 시간보다 이전 시간으로는 예약할 수 없습니다.");
        }

        // [로직 2] 최대 1주일 이내만 예약 가능
        if (targetDate.isAfter(now.toLocalDate().plusWeeks(1))) {
            throw new RuntimeException("예약은 최대 1주일 이내만 가능합니다.");
        }

        // [로직 3] 사용자 중복 예약 체크
        // 하루 한 번 제한 로직 유지 (시작~종료 범위 내 겹침 확인)
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.atTime(23, 59, 59);

        boolean alreadyReservedToday = reservationRepository.existsByUserIdAndOverlapTime(
                user.getId(), facility.getId(), startOfDay, endOfDay);

        if (alreadyReservedToday) {
            throw new RuntimeException("해당 시설은 하루에 한 번만 예약 가능합니다.");
        }

        // [로직 4] 시설 시간대 겹침 체크 (DB 쿼리에서 처리)
        List<Reservation> conflicting = reservationRepository.findConflictingReservations(
                facility.getId(), start, end);

        if (!conflicting.isEmpty()) {
            throw new RuntimeException("해당 시간대에 이미 예약(또는 승인대기)이 존재합니다.");
        }

        // --- 엔티티 생성 및 저장 ---
        Reservation reservation = Reservation.builder()
                .facility(facility)
                .user(user)
                .reservationTime(new ReservationTime(start, end))
                .status(ReservationStatus.PENDING)
                .build();

        return reservationRepository.save(reservation).getId();
    }

    /*
     * 특정 예약 조회
     */
    public ReservationResponseDto findOne(Long id, Long currentUserId, User.UserRole currentUserRole) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 예약을 찾을 수 없습니다."));

        // 본인 예약이 아니면서 관리자도 아닌 경우 조회 차단
        if (!reservation.getUser().getId().equals(currentUserId) &&
                !currentUserRole.equals(User.UserRole.ADMIN)) {
            throw new RuntimeException("조회 권한이 없습니다.");
        }

        return ReservationResponseDto.fromEntity(reservation);
    }

    /*
     * 모든 예약 조회
     */
    public List<ReservationResponseDto> findAll() {
        return reservationRepository.findAll().stream()
                .map(ReservationResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /*
     *  예약 승인 (관리자용)
     */
    @Transactional
    public void approve(Long id) {
        // 1. 해당 예약이 존재하는지 확인
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 예약을 찾을 수 없습니다."));

        // 2. 이미 승인되었거나 취소된 건지 확인 (방어 로직)
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            throw new RuntimeException("이미 승인된 예약입니다.");
        }
        if (reservation.getStatus() == ReservationStatus.CANCELED) {
            throw new RuntimeException("취소된 예약은 승인할 수 없습니다.");
        }

        // 3. 상태 변경 (CONFIRMED로 변경)
        reservation.approve();
        // 4. 저장
        reservationRepository.save(reservation);
    }

    /*
     *  예약 취소
     */
    @Transactional
    public void cancel(Long reservationId, Long currentUserId, User.UserRole currentUserRole) {
        // 1. 해당 예약이 존재하는지 확인 및 영속성 컨텍스트에 로드
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("해당 예약을 찾을 수 없습니다."));

        // 2. 이미 취소된 건지 확인하는 방어 로직 추가
        if (reservation.getStatus() == ReservationStatus.CANCELED) {
            throw new RuntimeException("이미 취소된 예약입니다.");
        }

        if (!reservation.getUser().getId().equals(currentUserId)
             && !currentUserRole.equals(User.UserRole.ADMIN)) {
            throw new RuntimeException("취소 권한이 없습니다.");
        }

        // 3. 상태 변경
        reservation.cancel();
        reservationRepository.save(reservation);
    }

    /*
        관리자용: 모든 예약 내역을 최신순으로 조회
     */
    public List<ReservationResponseDto> findAllWithDetails() {
        return reservationRepository.findAll().stream()
                .sorted((r1, r2) -> r2.getId().compareTo(r1.getId()))
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
                .stream()
                .map(ReservationResponseDto::fromEntity)
                .collect(Collectors.toList());
    }
}