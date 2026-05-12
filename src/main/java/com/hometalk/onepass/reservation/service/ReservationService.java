package com.hometalk.onepass.reservation.service;

import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.auth.repository.UserRepository;
import com.hometalk.onepass.facility.entity.Facility;
import com.hometalk.onepass.facility.repository.FacilityRepository;
import com.hometalk.onepass.notification.entity.NotificationTargetRole;
import com.hometalk.onepass.notification.entity.NotificationType;
import com.hometalk.onepass.notification.publisher.NotificationPublisher;
import com.hometalk.onepass.reservation.dto.ReservationCalendarDto;
import com.hometalk.onepass.reservation.dto.ReservationRequestDto;
import com.hometalk.onepass.reservation.dto.ReservationResponseDto;
import com.hometalk.onepass.reservation.entity.Reservation;
import com.hometalk.onepass.reservation.entity.ReservationStatus;
import com.hometalk.onepass.reservation.entity.ReservationTime;
import com.hometalk.onepass.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final FacilityRepository facilityRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final NotificationPublisher notificationPublisher;

    @Transactional
    public Long register(ReservationRequestDto dto, Long loginUserId) {
        Facility facility = facilityRepository.findById(dto.getFacilityId())
                .orElseThrow(() -> new RuntimeException("해당 시설을 찾을 수 없습니다."));

        User user = userRepository.findById(loginUserId)
                .orElseThrow(() -> new RuntimeException("해당 회원을 찾을 수 없습니다."));

        LocalDateTime start = dto.getStartTime();
        LocalDateTime end = dto.getEndTime();
        LocalDateTime now = LocalDateTime.now();
        LocalDate targetDate = start.toLocalDate();

        if (start.isBefore(now)) {
            throw new RuntimeException("현재 시간보다 이전 시간으로는 예약할 수 없습니다.");
        }

        if (targetDate.isAfter(now.toLocalDate().plusWeeks(1))) {
            throw new RuntimeException("예약은 최대 1주일 이내만 가능합니다.");
        }

        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.atTime(23, 59, 59);

        boolean alreadyReservedToday = reservationRepository.existsByUserIdAndOverlapTime(
                user.getId(), facility.getId(), startOfDay, endOfDay
        );

        if (alreadyReservedToday) {
            throw new RuntimeException("해당 시설은 하루에 한 번만 예약 가능합니다.");
        }

        List<Reservation> conflicting = reservationRepository.findConflictingReservations(
                facility.getId(), start, end
        );

        if (!conflicting.isEmpty()) {
            throw new RuntimeException("해당 시간대에 이미 예약 또는 승인대기 예약이 존재합니다.");
        }

        Reservation reservation = Reservation.builder()
                .facility(facility)
                .user(user)
                .reservationTime(new ReservationTime(start, end))
                .status(ReservationStatus.PENDING)
                .build();

        return reservationRepository.save(reservation).getId();
    }

    public ReservationResponseDto findOne(Long id, Long currentUserId, User.UserRole currentUserRole) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 예약을 찾을 수 없습니다."));

        if (!reservation.getUser().getId().equals(currentUserId)
                && !currentUserRole.equals(User.UserRole.ADMIN)) {
            throw new RuntimeException("조회 권한이 없습니다.");
        }

        return ReservationResponseDto.fromEntity(reservation);
    }

    public List<ReservationResponseDto> findAll() {
        return reservationRepository.findAll()
                .stream()
                .map(ReservationResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void approve(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 예약을 찾을 수 없습니다."));

        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            throw new RuntimeException("이미 승인된 예약입니다.");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELED) {
            throw new RuntimeException("취소된 예약은 승인할 수 없습니다.");
        }

        reservation.approve();
        reservationRepository.save(reservation);

        if (reservation.getUser() != null) {
            notificationPublisher.publish(
                    reservation.getUser().getId(),
                    NotificationTargetRole.RESIDENT,
                    NotificationType.RESERVATION_CONFIRMED,
                    "시설 예약 확정",
                    reservation.getFacility().getName() + " 예약이 확정되었습니다.",
                    "/reservation/" + reservation.getId(),
                    reservation.getId()
            );
        }
    }

    @Transactional
    public void cancel(Long reservationId,
                       Long currentUserId,
                       User.UserRole currentUserRole,
                       String reason) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("해당 예약을 찾을 수 없습니다."));

        if (reservation.getStatus() == ReservationStatus.CANCELED) {
            throw new RuntimeException("이미 취소된 예약입니다.");
        }

        if (!reservation.getUser().getId().equals(currentUserId)
                && !currentUserRole.equals(User.UserRole.ADMIN)) {
            throw new RuntimeException("취소 권한이 없습니다.");
        }

        // 관리자 취소
        if (currentUserRole.equals(User.UserRole.ADMIN)) {

            if (reason == null || reason.trim().isEmpty()) {
                throw new RuntimeException("취소 사유를 입력해주세요.");
            }

            reservation.adminCancel(reason);

            // 사용자 알림
            notificationPublisher.publish(
                    reservation.getUser().getId(),
                    NotificationTargetRole.RESIDENT,
                    NotificationType.RESERVATION_CANCELLED,
                    "예약이 취소되었습니다.",
                    "관리자에 의해 예약이 취소되었습니다. 사유: " + reason,
                    "/reservation/my",
                    reservation.getId()
            );

        } else {
            // 일반 사용자 취소
            reservation.cancel();
        }

        reservationRepository.save(reservation);
    }

    @Transactional
    public void finishUsage(Long id, Long currentUserId) {
        Reservation res = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("예약 내역을 찾을 수 없습니다."));
        if (!res.getUser().getId().equals(currentUserId)) {
            throw new RuntimeException("본인의 예약만 종료할 수 있습니다.");
        }
        if (res.getStatus() != ReservationStatus.CONFIRMED) {
            throw new RuntimeException("현재 이용 중인(확정된) 예약만 종료할 수 있습니다.");
        }

        LocalDateTime now = LocalDateTime.now();

//        if (now.plusMinutes(5).isBefore(res.getStartTime())) {
//            throw new RuntimeException("이용 시작 후에만 종료할 수 있습니다.");
//        }

        res.finish();

        LocalDateTime newEndTime = now.getMinute() == 0
                ? now.withSecond(0).withNano(0)
                : now.plusHours(1).withMinute(0).withSecond(0).withNano(0);

        if (newEndTime.isBefore(res.getEndTime())) {
            res.updateEndTime(newEndTime);
        }
    }

    public Page<ReservationResponseDto> findByUserId(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        return reservationRepository.findByUserIdOrderByIdDesc(userId, pageable)
                .map(ReservationResponseDto::fromEntity);
    }

    // 관리자 통계 데이터
    @Transactional(readOnly = true)
    public Map<String, Long> getAdminDashboardStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay(); // 오늘 00:00:00
        LocalDateTime endOfDay = now.toLocalDate().atTime(23, 59, 59); // 오늘 23:59:59
        Map<String, Long> stats = new HashMap<>();

        // 1. 승인 대기 건수
        stats.put("pendingCount", reservationRepository.countByStatus(ReservationStatus.PENDING));
        // 2. 현재 이용 중 건수 (확정 상태 + 현재 시간이 시작/종료 사이)
        stats.put("activeCount", reservationRepository.countActiveReservations(now));
        // 3. 오늘 전체 예약 건수
        stats.put("todayTotalCount", reservationRepository.countTodayReservations(startOfDay, endOfDay));

        return stats;
    }

    public Page<ReservationResponseDto> findAllFiltered(ReservationStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return reservationRepository.findAllByStatus(status, pageable)
                .map(ReservationResponseDto::fromEntity);
    }



    // 캘린더
    public List<ReservationCalendarDto> getCalendar(Long facilityId, int year, int month) {
        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime end = start.plusMonths(1).minusSeconds(1);

        return reservationRepository.findByFacilityIdAndMonthRange(facilityId, start, end)
                .stream()
                .map(ReservationCalendarDto::from)
                .collect(Collectors.toList());
    }

    // Top5 요약
    public List<ReservationResponseDto> findMyRecent(Long userId) {
        return reservationRepository.findTop5ByUser_IdOrderByIdDesc(userId)
                .stream()
                .map(ReservationResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    //
    public List<ReservationResponseDto> findAdminRecent() {
        return reservationRepository.findTop10ByOrderByIdDesc()
                .stream()
                .map(ReservationResponseDto::fromEntity)
                .collect(Collectors.toList());

    }
}