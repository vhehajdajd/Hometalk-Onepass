package com.hometalk.onepass.parking.service;

import com.hometalk.onepass.auth.entity.Household;
import com.hometalk.onepass.auth.repository.HouseholdRepository;
import com.hometalk.onepass.parking.dto.request.EntryRequest;
import com.hometalk.onepass.parking.dto.request.ManualEntryRequest;
import com.hometalk.onepass.parking.dto.response.VehicleSearchResult;
import com.hometalk.onepass.parking.entity.ParkingLog;
import com.hometalk.onepass.parking.entity.Vehicle;
import com.hometalk.onepass.parking.entity.VisitReservation;
import com.hometalk.onepass.parking.repository.ParkingLogRepository;
import com.hometalk.onepass.parking.repository.VehicleRepository;
import com.hometalk.onepass.parking.repository.VisitReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffEntryService {

    private final VehicleRepository vehicleRepository;
    private final VisitReservationRepository visitReservationRepository;
    private final HouseholdRepository householdRepository;
    private final ParkingLogRepository parkingLogRepository;

    // ─── 차량 조회 ───────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<VehicleSearchResult> search(String keyword) {
        String last4 = keyword.replace(" ", "");

        if (last4.length() != 4) return List.of();

        List<VehicleSearchResult> results = new ArrayList<>();

        vehicleRepository.findApprovedByLast4(last4)
                .stream()
                .map(VehicleSearchResult::ofResident)
                .forEach(results::add);

        visitReservationRepository.findTodayReservedByLast4(last4, LocalDate.now())
                .stream()
                .map(VehicleSearchResult::ofReservation)
                .forEach(results::add);

        return results;
    }

    // ─── 입차 처리 ───────────────────────────────────────────────
    @Transactional
    public void processEntry(EntryRequest request) {
        switch (request.getType()) {
            case RESERVATION -> {
                VisitReservation reservation = visitReservationRepository
                        .findById(request.getId())
                        .orElseThrow(() -> new IllegalArgumentException("예약 정보를 찾을 수 없습니다."));

                // 1. 예약 상태 검증
                if (reservation.getStatus() != VisitReservation.ReservationStatus.RESERVED
                        && reservation.getStatus() != VisitReservation.ReservationStatus.PENDING_CONFIRM) {
                    throw new IllegalStateException("입차 처리할 수 없는 예약 상태입니다.");
                }

                // 2. 차량번호 정규화
                String vehicleNumber = reservation.getVehicleNumber().replace(" ", "");

                // 3. 입차 중복 방지
                parkingLogRepository
                        .findByVehicleNumberAndStatus(vehicleNumber, ParkingLog.ParkingStatus.PARKED)
                        .ifPresent(l -> {
                            throw new IllegalStateException("이미 입차된 차량입니다.");
                        });

                // ParkingLog 생성자에서 reservation.enter() 처리
                ParkingLog log = new ParkingLog(
                        null,
                        vehicleNumber,
                        reservation.getHousehold(),
                        reservation,
                        null, // TODO: JWT 연동 후 staff 추출
                        ParkingLog.EntryType.RESERVATION
                );
                parkingLogRepository.save(log);
                // TODO: 알림 - "방문 차량 입차 완료"
            }
            case RESIDENT -> {
                Vehicle vehicle = vehicleRepository
                        .findById(request.getId())
                        .orElseThrow(() -> new IllegalArgumentException("차량 정보를 찾을 수 없습니다."));

                if (vehicle.getStatus() != Vehicle.VehicleStatus.APPROVED) {
                    throw new IllegalStateException("승인된 차량만 입차 처리할 수 있습니다.");
                }

                // 차량번호 정규화
                String vehicleNumber = vehicle.getVehicleNumber().replace(" ", "");

                // 입차 중복 방지
                parkingLogRepository
                        .findByVehicleNumberAndStatus(vehicleNumber, ParkingLog.ParkingStatus.PARKED)
                        .ifPresent(l -> {
                            throw new IllegalStateException("이미 입차된 차량입니다.");
                        });

                ParkingLog log = new ParkingLog(
                        vehicle,
                        vehicleNumber,
                        vehicle.getHousehold(),
                        null,
                        null, // TODO: JWT 연동 후 staff 추출
                        ParkingLog.EntryType.NORMAL
                );
                parkingLogRepository.save(log);
                // TODO: 알림 - "입주자 차량 입차 완료"
            }
        }
    }

    // ─── 수동 입차 ───────────────────────────────────────────────
    @Transactional
    public void processManualEntry(ManualEntryRequest request) {
        Household household = null;

        if (hasText(request.getDong()) && hasText(request.getHo())) {
            String dong = request.getDong().endsWith("동") ? request.getDong() : request.getDong() + "동";
            String ho = request.getHo().endsWith("호") ? request.getHo() : request.getHo() + "호";

            household = householdRepository
                    .findByDongAndHo(dong, ho)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 세대입니다."));
        }

        // 차량번호 정규화
        String vehicleNumber = request.getVehicleNumber().replace(" ", "");

        // 입차 중복 방지
        parkingLogRepository
                .findByVehicleNumberAndStatus(vehicleNumber, ParkingLog.ParkingStatus.PARKED)
                .ifPresent(l -> {
                    throw new IllegalStateException("이미 입차된 차량입니다.");
                });

        VisitReservation reservation = VisitReservation.ofManual(
                household,
                vehicleNumber,
                request.getPurposeType()
        );
        visitReservationRepository.save(reservation);

        ParkingLog log = new ParkingLog(
                null,
                vehicleNumber,
                household,
                reservation,
                null, // TODO: JWT 연동 후 staff 추출
                ParkingLog.EntryType.MANUAL
        );
        parkingLogRepository.save(log);
        // TODO: 알림 - "미등록 차량 입차됨, 방문자로 등록하시겠어요?"
    }

    // ─── 오늘 방문 예정 목록 ─────────────────────────────────────
    @Transactional(readOnly = true)
    public List<VisitReservation> getTodayVisitList() {
        return visitReservationRepository.findTodayReserved(LocalDate.now());
    }

    // ─── 입주자 차량 목록 ────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<Vehicle> getResidentVehicleList() {
        List<Vehicle> approvedVehicles = vehicleRepository.findAllByStatusWithHousehold(Vehicle.VehicleStatus.APPROVED);

        // 현재 주차 중인 차량 번호 목록
        List<String> parkedVehicleNumbers = parkingLogRepository
                .findByStatus(ParkingLog.ParkingStatus.PARKED)
                .stream()
                .map(ParkingLog::getVehicleNumber)
                .toList();

        // 주차 중인 차량 제외
        return approvedVehicles.stream()
                .filter(v -> !parkedVehicleNumbers.contains(
                        v.getVehicleNumber().replace(" ", "")))
                .toList();
    }

    // ─── 유틸 ────────────────────────────────────────────────────
    private boolean hasText(String str) {
        return str != null && !str.isBlank();
    }

}