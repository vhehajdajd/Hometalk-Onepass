package com.hometalk.onepass.parking.service;

import com.hometalk.onepass.auth.entity.Household;
import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.notification.entity.NotificationTargetRole;
import com.hometalk.onepass.notification.entity.NotificationType;
import com.hometalk.onepass.notification.publisher.NotificationPublisher;
import com.hometalk.onepass.parking.dto.response.ParkingLogResponse;
import com.hometalk.onepass.parking.entity.ParkingLog;
import com.hometalk.onepass.parking.entity.ParkingTicket;
import com.hometalk.onepass.parking.exception.ParkingException;
import com.hometalk.onepass.parking.repository.ParkingLogRepository;
import com.hometalk.onepass.parking.repository.ParkingTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffExitService {

    private final ParkingLogRepository    parkingLogRepository;
    private final ParkingTicketRepository parkingTicketRepository;
    private final NotificationPublisher   notificationPublisher;

    @Transactional(readOnly = true)
    public List<ParkingLogResponse> search(String keyword) {
        String last4 = keyword.replace(" ", "");
        if (last4.length() != 4) return List.of();

        return parkingLogRepository.findParkedByLast4(last4)
                .stream()
                .map(log -> new ParkingLogResponse(log, getAvailableMinutes(log)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ParkingLogResponse> getParkedVisitList() {
        return parkingLogRepository.findValidParkedLogs()
                .stream()
                .filter(log -> log.getEntryType() == ParkingLog.EntryType.RESERVATION
                        || log.getEntryType() == ParkingLog.EntryType.MANUAL)
                .map(log -> new ParkingLogResponse(log, getAvailableMinutes(log)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ParkingLogResponse> getParkedResidentList() {
        return parkingLogRepository.findValidParkedLogs()
                .stream()
                .filter(log -> log.getEntryType() == ParkingLog.EntryType.NORMAL)
                .map(log -> new ParkingLogResponse(log, getAvailableMinutes(log)))
                .toList();
    }

    @Transactional
    public void processExit(Long parkingId) {
        ParkingLog parkingLog = parkingLogRepository.findByIdWithLock(parkingId)
                .orElseThrow(() -> new ParkingException("주차 기록을 찾을 수 없습니다."));

        if (parkingLog.getStatus() != ParkingLog.ParkingStatus.PARKED) {
            throw new ParkingException("이미 출차된 차량입니다.");
        }
        if (parkingLog.getHousehold() == null) {
            throw new ParkingException("세대 미확인 차량입니다. 강제 출차 처리해주세요.");
        }

        int totalMinutes = (int) Duration.between(
                parkingLog.getEntryTime(), LocalDateTime.now()).toMinutes();

        // 10분 이내 무료 출차
        if (totalMinutes <= 10) {
            parkingLog.exit(totalMinutes, totalMinutes);
            return;
        }

        // 입주자 차량 — 티켓 체크 없이 바로 출차
        if (parkingLog.getEntryType() == ParkingLog.EntryType.NORMAL) {
            parkingLog.exit(totalMinutes, totalMinutes);

            List<Long> residentUserIds = parkingLog.getHousehold().getUsers().stream()
                    .filter(u -> u.getRole() == User.UserRole.RESIDENT)
                    .map(User::getId)
                    .toList();
            String vehicleNumber = parkingLog.getVehicleNumber();
            Long pId = parkingLog.getParkingId();

            residentUserIds.forEach(userId -> notificationPublisher.publish(
                    userId,
                    NotificationTargetRole.RESIDENT,
                    NotificationType.VEHICLE_EXIT,
                    "출차 완료",
                    "차량(" + vehicleNumber + ")이 출차하였습니다.",
                    "/parking/vehicle",
                    pId
            ));
            return;
        }

        // 방문/수동 차량 — 티켓 체크
        int applied = parkingLog.getAppliedMinutes() != null
                ? parkingLog.getAppliedMinutes() : 0;

        if (applied < totalMinutes) {
            List<Long> residentUserIds = parkingLog.getHousehold().getUsers().stream()
                    .filter(u -> u.getRole() == User.UserRole.RESIDENT)
                    .map(User::getId)
                    .toList();
            Long pId = parkingLog.getParkingId();

            residentUserIds.forEach(userId -> notificationPublisher.publishAsync(
                    userId,
                    NotificationTargetRole.RESIDENT,
                    NotificationType.VEHICLE_TICKET_SHORTAGE,
                    "티켓 부족",
                    "티켓이 부족하여 출차할 수 없습니다. 티켓을 적용해 주세요.",
                    "/parking/ticket",
                    pId
            ));

            throw new ParkingException("티켓이 부족합니다. 티켓을 먼저 등록해주세요.");
        }

        parkingLog.exit(totalMinutes, Math.min(applied, totalMinutes));

        List<Long> residentUserIds = parkingLog.getHousehold().getUsers().stream()
                .filter(u -> u.getRole() == User.UserRole.RESIDENT)
                .map(User::getId)
                .toList();
        String vehicleNumber = parkingLog.getVehicleNumber();
        Long pId = parkingLog.getParkingId();

        residentUserIds.forEach(userId -> notificationPublisher.publishAsync(
                userId,
                NotificationTargetRole.RESIDENT,
                NotificationType.VEHICLE_VISITOR_EXIT,
                "방문 차량 출차",
                "방문 차량(" + vehicleNumber + ")이 출차하였습니다.",
                "/parking/vehicle",
                pId
        ));
    }

    @Transactional(readOnly = true)
    public void sendTicketShortageNotification(Long parkingId) {
        ParkingLog parkingLog = parkingLogRepository.findById(parkingId)
                .orElseThrow(() -> new ParkingException("주차 기록을 찾을 수 없습니다."));

        if (parkingLog.getHousehold() == null) return;

        parkingLog.getHousehold().getUsers().stream()
                .filter(u -> u.getRole() == User.UserRole.RESIDENT)
                .forEach(u -> notificationPublisher.publishAsync(
                        u.getId(),
                        NotificationTargetRole.RESIDENT,
                        NotificationType.VEHICLE_TICKET_SHORTAGE,
                        "티켓 부족",
                        "티켓이 부족하여 출차할 수 없습니다. 티켓을 적용해 주세요.",
                        "/parking/ticket",
                        parkingLog.getParkingId()
                ));
    }

    @Transactional
    public void forceExit(Long parkingId) {
        ParkingLog parkingLog = parkingLogRepository.findByIdWithLock(parkingId)
                .orElseThrow(() -> new ParkingException("주차 기록을 찾을 수 없습니다."));

        if (parkingLog.getStatus() != ParkingLog.ParkingStatus.PARKED) {
            throw new ParkingException("이미 출차된 차량입니다.");
        }

        int totalMinutes = (int) Duration.between(
                parkingLog.getEntryTime(), LocalDateTime.now()).toMinutes();
        int appliedMinutes = Math.min(
                parkingLog.getAppliedMinutes() != null ? parkingLog.getAppliedMinutes() : 0,
                totalMinutes);

        parkingLog.exit(totalMinutes, appliedMinutes);
        log.info("강제 출차 처리 - parkingId: {}, 현장 결제 완료", parkingId);
    }

    private int getAvailableMinutes(ParkingLog parkingLog) {
        if (parkingLog.getHousehold() == null) return 0;

        Household household = parkingLog.getHousehold();
        LocalDate today = LocalDate.now();
        int availableMinutes = 0;

        Optional<ParkingTicket> dayTicketOpt = parkingTicketRepository
                .findByHouseholdAndTypeAndIssueYearAndIssueMonth(
                        household, ParkingTicket.TicketType.DAY,
                        today.getYear(), today.getMonthValue());
        if (dayTicketOpt.isPresent()) {
            availableMinutes += dayTicketOpt.get().getRemainingCount()
                    * ParkingTicket.TicketType.DAY.toMinutes(1);
        }

        Optional<ParkingTicket> hourTicketOpt = parkingTicketRepository
                .findByHouseholdAndTypeAndIssueYearAndIssueMonth(
                        household, ParkingTicket.TicketType.HOUR,
                        today.getYear(), today.getMonthValue());
        if (hourTicketOpt.isPresent()) {
            availableMinutes += hourTicketOpt.get().getRemainingCount()
                    * ParkingTicket.TicketType.HOUR.toMinutes(1);
        }

        return availableMinutes;
    }
}