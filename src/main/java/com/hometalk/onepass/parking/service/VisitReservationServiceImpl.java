package com.hometalk.onepass.parking.service;

import com.hometalk.onepass.auth.entity.Household;
import com.hometalk.onepass.auth.repository.HouseholdRepository;
import com.hometalk.onepass.parking.dto.request.VisitReservationRequest;
import com.hometalk.onepass.parking.dto.response.VisitReservationResponse;
import com.hometalk.onepass.parking.entity.VisitReservation;
import com.hometalk.onepass.parking.repository.VisitReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class VisitReservationServiceImpl implements VisitReservationService {

    private final VisitReservationRepository visitReservationRepository;
    private final HouseholdRepository householdRepository;

    // 방문 예약 등록
    @Override
    public VisitReservationResponse register(Long householdId, VisitReservationRequest request) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new EntityNotFoundException("세대를 찾을 수 없습니다."));

        if (request.getReservedAt() == null || request.getReservedAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("예약 시간은 현재 이후여야 합니다.");
        }

        // 차량번호 공백 제거 정규화
        String vehicleNumber = request.getVehicleNumber().replace(" ", "");

        if (visitReservationRepository.existsByVehicleNumberAndReservedAt(
                vehicleNumber, request.getReservedAt())) {
            throw new IllegalArgumentException("이미 같은 시간에 예약된 차량입니다.");
        }

        VisitReservation reservation = new VisitReservation(
                household,
                vehicleNumber,
                request.getPurpose(),
                request.getReservedAt()
        );
        visitReservationRepository.save(reservation);

        return new VisitReservationResponse(reservation);
    }

    // 방문 예약 단건 조회
    @Override
    @Transactional(readOnly = true)
    public VisitReservationResponse getReservation(Long reservationId) {
        VisitReservation reservation = visitReservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("예약을 찾을 수 없습니다."));
        return new VisitReservationResponse(reservation);
    }

    // 방문 예약 수정
    @Override
    public VisitReservationResponse update(Long reservationId, VisitReservationRequest request) {
        VisitReservation reservation = visitReservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("예약을 찾을 수 없습니다."));

        // 차량번호 공백 제거 정규화
        String vehicleNumber = request.getVehicleNumber() != null
                ? request.getVehicleNumber().replace(" ", "")
                : null;

        reservation.update(vehicleNumber, request.getPurpose(), request.getReservedAt());

        return new VisitReservationResponse(reservation);
    }

    // 방문 예약 취소
    @Override
    public void cancel(Long reservationId) {
        VisitReservation reservation = visitReservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("예약을 찾을 수 없습니다."));

        reservation.cancel();
    }

    // 입차 처리
    @Override
    public void enter(Long reservationId) {
        VisitReservation reservation = visitReservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("예약을 찾을 수 없습니다."));

        reservation.enter();
    }

    // 세대별 예약 목록 조회
    // RESERVED: 항상 노출 + 정렬 최상단
    // ENTERED: 당일만 노출
    // CANCELLED: 7일간 노출
    @Override
    @Transactional(readOnly = true)
    public List<VisitReservationResponse> getHouseholdReservations(Long householdId) {
        LocalDate today = LocalDate.now();
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        return visitReservationRepository
                .findHouseholdReservationsFiltered(householdId, today, sevenDaysAgo)
                .stream()
                .map(VisitReservationResponse::new)
                .collect(Collectors.toList());
    }

    // 상태별 조회
    @Override
    @Transactional(readOnly = true)
    public List<VisitReservationResponse> getHouseholdReservationsByStatus(
            Long householdId,
            VisitReservation.ReservationStatus status) {
        return visitReservationRepository.findByHousehold_IdAndStatus(householdId, status)
                .stream()
                .map(VisitReservationResponse::new)
                .collect(Collectors.toList());
    }

    // 수동 입차 목록 조회 (PENDING_CONFIRM)
    @Override
    @Transactional(readOnly = true)
    public List<VisitReservationResponse> getPendingConfirmReservations(Long householdId) {
        return visitReservationRepository
                .findByHousehold_IdAndStatus(householdId, VisitReservation.ReservationStatus.PENDING_CONFIRM)
                .stream()
                .map(VisitReservationResponse::new)
                .collect(Collectors.toList());
    }
}