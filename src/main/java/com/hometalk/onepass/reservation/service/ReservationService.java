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
    public void cancel(Long reservationId, Long currentUserId, User.UserRole currentUserRole) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("해당 예약을 찾을 수 없습니다."));

        if (reservation.getStatus() == ReservationStatus.CANCELED) {
            throw new RuntimeException("이미 취소된 예약입니다.");
        }

        if (!reservation.getUser().getId().equals(currentUserId)
                && !currentUserRole.equals(User.UserRole.ADMIN)) {
            throw new RuntimeException("취소 권한이 없습니다.");
        }

        reservation.cancel();
        reservationRepository.save(reservation);
    }

    public List<ReservationResponseDto> findAllWithDetails() {
        return reservationRepository.findAll()
                .stream()
                .sorted((r1, r2) -> r2.getId().compareTo(r1.getId()))
                .map(ReservationResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ReservationCalendarDto> getCalendar(int year, int month) {
        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime end = start.plusMonths(1).minusSeconds(1);

        return reservationRepository.findByMonthRange(start, end)
                .stream()
                .map(ReservationCalendarDto::from)
                .collect(Collectors.toList());
    }

    public List<ReservationResponseDto> findByUserId(Long userId) {
        return reservationRepository.findByUserIdOrderByIdDesc(userId)
                .stream()
                .map(ReservationResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ReservationResponseDto> findMyRecent(Long userId) {
        return reservationRepository.findTop5ByUser_IdOrderByIdDesc(userId)
                .stream()
                .map(ReservationResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ReservationResponseDto> findAdminRecent() {
        return reservationRepository.findTop10ByOrderByIdDesc()
                .stream()
                .map(ReservationResponseDto::fromEntity)
                .collect(Collectors.toList());
    }
}