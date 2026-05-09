package com.hometalk.onepass.parking.service;

import com.hometalk.onepass.auth.entity.Household;
import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.auth.repository.HouseholdRepository;
import com.hometalk.onepass.notification.entity.NotificationTargetRole;
import com.hometalk.onepass.notification.entity.NotificationType;
import com.hometalk.onepass.notification.publisher.NotificationPublisher;
import com.hometalk.onepass.parking.dto.request.EntryRequest;
import com.hometalk.onepass.parking.dto.request.ManualEntryRequest;
import com.hometalk.onepass.parking.dto.response.VehicleSearchResult;
import com.hometalk.onepass.parking.entity.ParkingLog;
import com.hometalk.onepass.parking.entity.Vehicle;
import com.hometalk.onepass.parking.entity.VisitReservation;
import com.hometalk.onepass.parking.exception.ParkingException;
import com.hometalk.onepass.parking.repository.ParkingLogRepository;
import com.hometalk.onepass.parking.repository.VehicleRepository;
import com.hometalk.onepass.parking.repository.VisitReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffEntryService {

    private final VehicleRepository vehicleRepository;
    private final VisitReservationRepository visitReservationRepository;
    private final HouseholdRepository householdRepository;
    private final ParkingLogRepository parkingLogRepository;
    private final NotificationPublisher notificationPublisher;

    @Transactional(readOnly = true)
    public List<VehicleSearchResult> search(String keyword) {
        String last4 = keyword.replace(" ", "");
        if (last4.length() != 4) return List.of();

        List<VehicleSearchResult> results = new ArrayList<>();
        vehicleRepository.findApprovedByLast4(last4)
                .stream().map(VehicleSearchResult::ofResident).forEach(results::add);
        visitReservationRepository.findTodayReservedByLast4(last4, LocalDate.now())
                .stream().map(VehicleSearchResult::ofReservation).forEach(results::add);
        return results;
    }

    @Transactional
    public void processEntry(EntryRequest request) {
        switch (request.getType()) {
            case RESERVATION -> {
                VisitReservation reservation = visitReservationRepository
                        .findById(request.getId())
                        .orElseThrow(() -> new ParkingException("예약 정보를 찾을 수 없습니다."));

                if (reservation.getStatus() != VisitReservation.ReservationStatus.RESERVED
                        && reservation.getStatus() != VisitReservation.ReservationStatus.PENDING_CONFIRM) {
                    throw new ParkingException("입차 처리할 수 없는 예약 상태입니다.");
                }

                LocalDateTime reservedAt = reservation.getReservedAt();
                LocalDateTime now = LocalDateTime.now();
                if (now.isBefore(reservedAt.minusMinutes(30)) || now.isAfter(reservedAt.plusMinutes(30))) {
                    throw new ParkingException("예약 시간 30분 전후에만 입차 가능합니다.");
                }

                String vehicleNumber = reservation.getVehicleNumber().replace(" ", "");
                parkingLogRepository
                        .findByVehicleNumberAndStatus(vehicleNumber, ParkingLog.ParkingStatus.PARKED)
                        .ifPresent(l -> { throw new ParkingException("이미 입차된 차량입니다."); });

                ParkingLog log = new ParkingLog(null, vehicleNumber,
                        reservation.getHousehold(), reservation, null,
                        ParkingLog.EntryType.RESERVATION);
                parkingLogRepository.save(log);

                // ✅ 알림 — ④ 예약 방문객 입차 (입주자에게 알림)
                reservation.getHousehold().getUsers().stream()
                        .filter(u -> u.getRole() == User.UserRole.RESIDENT)
                        .forEach(u -> notificationPublisher.publish(
                                u.getId(),
                                NotificationTargetRole.RESIDENT,
                                NotificationType.VEHICLE_VISITOR_ENTRY,
                                "방문 차량 입차",
                                "예약하신 방문 차량(" + reservation.getVehicleNumber() + ")이 도착하여 입차했습니다.",
                                "/parking/vehicle",
                                reservation.getReservationId()
                        ));
            }

            case RESIDENT -> {
                Vehicle vehicle = vehicleRepository
                        .findById(request.getId())
                        .orElseThrow(() -> new ParkingException("차량 정보를 찾을 수 없습니다."));

                if (!Vehicle.VehicleStatus.APPROVED.equals(vehicle.getStatus())) {
                    throw new ParkingException("승인된 차량만 입차 처리할 수 있습니다.");
                }

                String vehicleNumber = vehicle.getVehicleNumber().replace(" ", "");
                parkingLogRepository
                        .findByVehicleNumberAndStatus(vehicleNumber, ParkingLog.ParkingStatus.PARKED)
                        .ifPresent(l -> { throw new ParkingException("이미 입차된 차량입니다."); });

                ParkingLog log = new ParkingLog(vehicle, vehicleNumber,
                        vehicle.getHousehold(), null, null,
                        ParkingLog.EntryType.NORMAL);
                parkingLogRepository.save(log);

                // ✅ 알림 — ③ 입주자 차량 입차
                notificationPublisher.publish(
                        vehicle.getUser().getId(),
                        NotificationTargetRole.RESIDENT,
                        NotificationType.VEHICLE_ENTRY,
                        "입주자 입차",
                        "입주자 차량(" + vehicle.getVehicleNumber() + ")이 입차했습니다.",
                        "/parking/vehicle",
                        vehicle.getVehicleId()
                );
            }
        }
    }

    @Transactional
    public void processManualEntry(ManualEntryRequest request, String postNum) {
        Household household = null;

        if (hasText(request.getDong()) && hasText(request.getHo())) {
            household = householdRepository.findByPostNumAndDongAndHo(
                            postNum, request.getDong(), request.getHo())
                    .orElse(null);
        }

        String vehicleNumber = request.getVehicleNumber().replace(" ", "");

        vehicleRepository.findByVehicleNumber(vehicleNumber)
                .ifPresent(v -> {
                    if (!Vehicle.VehicleStatus.APPROVED.equals(v.getStatus())) {
                        throw new ParkingException("승인되지 않은 등록 차량은 입차할 수 없습니다.");
                    }
                });

        parkingLogRepository
                .findByVehicleNumberAndStatus(vehicleNumber, ParkingLog.ParkingStatus.PARKED)
                .ifPresent(l -> { throw new ParkingException("이미 입차된 차량입니다."); });

        VisitReservation reservation = VisitReservation.ofManual(
                household, vehicleNumber, request.getPurposeType());
        visitReservationRepository.save(reservation);

        parkingLogRepository.save(new ParkingLog(null, vehicleNumber,
                household, reservation, null, ParkingLog.EntryType.MANUAL));
    }

    @Transactional(readOnly = true)
    public List<VisitReservation> getTodayVisitList() {
        return visitReservationRepository.findTodayReserved(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<Vehicle> getResidentVehicleList() {
        List<Vehicle> approvedVehicles = vehicleRepository
                .findAllByStatusWithHousehold(Vehicle.VehicleStatus.APPROVED);

        List<String> parkedVehicleNumbers = parkingLogRepository
                .findByStatus(ParkingLog.ParkingStatus.PARKED)
                .stream().map(ParkingLog::getVehicleNumber).toList();

        return approvedVehicles.stream()
                .filter(v -> !parkedVehicleNumbers.contains(
                        v.getVehicleNumber().replace(" ", "")))
                .toList();
    }

    private boolean hasText(String str) {
        return str != null && !str.isBlank();
    }
}