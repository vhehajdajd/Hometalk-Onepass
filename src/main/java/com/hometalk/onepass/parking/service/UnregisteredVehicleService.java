package com.hometalk.onepass.parking.service;

import com.hometalk.onepass.auth.entity.Household;
import com.hometalk.onepass.auth.repository.HouseholdRepository;
import com.hometalk.onepass.parking.dto.response.UnregisteredVehicleResponse;
import com.hometalk.onepass.parking.entity.ParkingLog;
import com.hometalk.onepass.parking.entity.VisitReservation;
import com.hometalk.onepass.parking.repository.ParkingLogRepository;
import com.hometalk.onepass.parking.repository.VisitReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UnregisteredVehicleService {

    private final VisitReservationRepository visitReservationRepository;
    private final HouseholdRepository householdRepository;
    private final ParkingLogRepository parkingLogRepository;

    // ─── 미등록 차량 검색 ─────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<UnregisteredVehicleResponse> search(String keyword) {
        String last4 = keyword.replace(" ", "");

        if (last4.length() != 4) return List.of();

        return visitReservationRepository.findUnregisteredByLast4(last4)
                .stream()
                .map(UnregisteredVehicleResponse::new)
                .toList();
    }

    // ─── 내 세대 방문자로 등록 ────────────────────────────────────
    @Transactional
    public void match(Long reservationId, Long householdId) {
        VisitReservation reservation = visitReservationRepository
                .findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("입차 정보를 찾을 수 없습니다."));

        Household household = householdRepository
                .findById(householdId)
                .orElseThrow(() -> new EntityNotFoundException("세대 정보를 찾을 수 없습니다."));

        // VisitReservation 세대 매칭
        reservation.matchHousehold(household);

        // ParkingLog 세대 매칭
        parkingLogRepository.findByReservationAndStatus(reservation, ParkingLog.ParkingStatus.PARKED)
                .ifPresent(parkingLog -> parkingLog.matchHousehold(household));
    }
}