package com.hometalk.onepass.parking.service;

import com.hometalk.onepass.auth.entity.Household;
import com.hometalk.onepass.parking.dto.response.ParkingLogResponse;
import com.hometalk.onepass.parking.entity.ParkingLog;
import com.hometalk.onepass.parking.entity.ParkingTicket;
import com.hometalk.onepass.parking.entity.TicketUsage;
import com.hometalk.onepass.parking.repository.ParkingLogRepository;
import com.hometalk.onepass.parking.repository.ParkingTicketRepository;
import com.hometalk.onepass.parking.repository.TicketUsageRepository;
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

    private final ParkingLogRepository parkingLogRepository;
    private final ParkingTicketRepository parkingTicketRepository;
    private final TicketUsageRepository ticketUsageRepository;

    // ─── 출차 차량 퀵서치 ────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<ParkingLogResponse> search(String keyword) {
        String last4 = keyword.replace(" ", "");

        if (last4.length() != 4) return List.of();

        return parkingLogRepository.findParkedByLast4(last4)
                .stream()
                .map(parkingLog -> {
                    int availableMinutes = getAvailableMinutes(parkingLog);
                    return new ParkingLogResponse(parkingLog, availableMinutes);
                })
                .toList();
    }

    // ─── 주차 중인 방문 차량 목록 ────────────────────────────────
    @Transactional(readOnly = true)
    public List<ParkingLogResponse> getParkedVisitList() {
        return parkingLogRepository.findByStatus(ParkingLog.ParkingStatus.PARKED)
                .stream()
                .filter(parkingLog -> parkingLog.getEntryType() == ParkingLog.EntryType.RESERVATION
                        || parkingLog.getEntryType() == ParkingLog.EntryType.MANUAL)
                .map(parkingLog -> {
                    int availableMinutes = getAvailableMinutes(parkingLog);
                    return new ParkingLogResponse(parkingLog, availableMinutes);
                })
                .toList();
    }

    // ─── 주차 중인 입주자 차량 목록 ──────────────────────────────
    @Transactional(readOnly = true)
    public List<ParkingLogResponse> getParkedResidentList() {
        return parkingLogRepository.findByStatus(ParkingLog.ParkingStatus.PARKED)
                .stream()
                .filter(parkingLog -> parkingLog.getEntryType() == ParkingLog.EntryType.NORMAL)
                .map(parkingLog -> {
                    int availableMinutes = getAvailableMinutes(parkingLog);
                    return new ParkingLogResponse(parkingLog, availableMinutes);
                })
                .toList();
    }

    // ─── 출차 처리 ───────────────────────────────────────────────
    @Transactional
    public void processExit(Long parkingId) {
        ParkingLog parkingLog = parkingLogRepository.findByIdWithLock(parkingId)
                .orElseThrow(() -> new IllegalArgumentException("주차 기록을 찾을 수 없습니다."));

        if (parkingLog.getStatus() != ParkingLog.ParkingStatus.PARKED) {
            throw new IllegalStateException("이미 출차된 차량입니다.");
        }

        // 세대 미확인 차량은 일반 출차 불가
        if (parkingLog.getHousehold() == null) {
            throw new IllegalStateException("세대 미확인 차량입니다. 강제 출차 처리해주세요.");
        }

        int totalMinutes = (int) Duration.between(
                parkingLog.getEntryTime(), LocalDateTime.now()).toMinutes();

        // 미리 적용된 티켓 시간으로 출차 가능 여부 확인
        int applied = parkingLog.getAppliedMinutes() != null
                ? parkingLog.getAppliedMinutes() : 0;

        if (applied < totalMinutes) {
            throw new IllegalStateException("티켓이 부족합니다. 티켓을 먼저 등록해주세요.");
        }

        parkingLog.exit(totalMinutes, Math.min(applied, totalMinutes));
        // TODO: 알림 - "출차 완료"
    }

    // ─── 강제 출차 처리 (현장 결제 완료 후) ─────────────────────
    @Transactional
    public void forceExit(Long parkingId) {
        ParkingLog parkingLog = parkingLogRepository.findByIdWithLock(parkingId)
                .orElseThrow(() -> new IllegalArgumentException("주차 기록을 찾을 수 없습니다."));

        if (parkingLog.getStatus() != ParkingLog.ParkingStatus.PARKED) {
            throw new IllegalStateException("이미 출차된 차량입니다.");
        }

        int totalMinutes = (int) Duration.between(
                parkingLog.getEntryTime(), LocalDateTime.now()).toMinutes();

        int appliedMinutes = parkingLog.getAppliedMinutes() != null
                ? parkingLog.getAppliedMinutes() : 0;

        // appliedMinutes가 totalMinutes 초과하지 않도록 보정
        appliedMinutes = Math.min(appliedMinutes, totalMinutes);

        parkingLog.exit(totalMinutes, appliedMinutes);
        log.info("강제 출차 처리 - parkingId: {}, 현장 결제 완료", parkingId);
        // TODO: 알림 - "출차 완료"
    }

    // ─── 사용 가능한 티켓 시간 계산 ─────────────────────────────
    private int getAvailableMinutes(ParkingLog parkingLog) {
        if (parkingLog.getHousehold() == null) return 0;

        Household household = parkingLog.getHousehold();
        LocalDate today = LocalDate.now();
        int availableMinutes = 0;

        // DAY권
        Optional<ParkingTicket> dayTicketOpt = parkingTicketRepository
                .findByHouseholdAndTypeAndIssueYearAndIssueMonth(
                        household, ParkingTicket.TicketType.DAY,
                        today.getYear(), today.getMonthValue());
        if (dayTicketOpt.isPresent()) {
            availableMinutes += dayTicketOpt.get().getRemainingCount()
                    * ParkingTicket.TicketType.DAY.toMinutes(1);
        }

        // HOUR권
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