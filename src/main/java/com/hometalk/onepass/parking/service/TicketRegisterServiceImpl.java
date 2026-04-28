package com.hometalk.onepass.parking.service;

import com.hometalk.onepass.auth.entity.Household;
import com.hometalk.onepass.auth.repository.HouseholdRepository;
import com.hometalk.onepass.parking.dto.request.TicketApplyRequest;
import com.hometalk.onepass.parking.dto.request.TicketCancelRequest;
import com.hometalk.onepass.parking.dto.response.ParkingSearchResponse;
import com.hometalk.onepass.parking.entity.ParkingLog;
import com.hometalk.onepass.parking.entity.ParkingTicket;
import com.hometalk.onepass.parking.entity.TicketUsage;
import com.hometalk.onepass.parking.repository.ParkingLogRepository;
import com.hometalk.onepass.parking.repository.ParkingTicketRepository;
import com.hometalk.onepass.parking.repository.TicketUsageRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TicketRegisterServiceImpl implements TicketRegisterService {

    private final ParkingLogRepository parkingLogRepository;
    private final ParkingTicketRepository parkingTicketRepository;
    private final TicketUsageRepository ticketUsageRepository;
    private final HouseholdRepository householdRepository;

    // ─── 차량 조회 ───────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public ParkingSearchResponse searchParkedVehicle(String keyword, Long householdId) {
        String last4 = keyword.replace(" ", "");

        if (last4.length() != 4) {
            throw new IllegalArgumentException("차량 번호 4자리를 입력해주세요.");
        }

        List<ParkingLog> logs = parkingLogRepository.findParkedByLast4(last4);

        if (logs.isEmpty()) {
            throw new EntityNotFoundException("주차 중인 차량이 없습니다.");
        }

        ParkingLog parkingLog = logs.get(0);

        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new EntityNotFoundException("세대를 찾을 수 없습니다."));

        LocalDate today = LocalDate.now();
        List<ParkingTicket> tickets = parkingTicketRepository
                .findByHouseholdAndIssueYearAndIssueMonth(
                        household, today.getYear(), today.getMonthValue());

        // 현재 적용된 티켓 사용 내역 조회
        List<TicketUsage> usages = ticketUsageRepository.findByParkingLog(parkingLog);

        return new ParkingSearchResponse(parkingLog, tickets, usages);
    }

    // ─── 티켓 적용 ───────────────────────────────────────────────
    @Override
    public void applyTicket(TicketApplyRequest request, Long householdId) {
        ParkingLog parkingLog = parkingLogRepository.findByIdWithLock(request.getParkingId())
                .orElseThrow(() -> new EntityNotFoundException("주차 기록을 찾을 수 없습니다."));

        if (parkingLog.getStatus() != ParkingLog.ParkingStatus.PARKED) {
            throw new IllegalStateException("주차 중인 차량이 아닙니다.");
        }

        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new EntityNotFoundException("세대를 찾을 수 없습니다."));

        ParkingTicket.TicketType ticketType =
                ParkingTicket.TicketType.valueOf(request.getTicketType());

        LocalDate today = LocalDate.now();

        ParkingTicket ticket = parkingTicketRepository
                .findByHouseholdAndTypeAndIssueYearAndIssueMonth(
                        household, ticketType, today.getYear(), today.getMonthValue())
                .orElseThrow(() -> new EntityNotFoundException("보유한 티켓이 없습니다."));

        if (!ticket.isEnough(request.getCount())) {
            throw new IllegalStateException("티켓 잔여 수량이 부족합니다.");
        }

        TicketUsage usage = new TicketUsage(parkingLog, ticket, request.getCount());
        ticketUsageRepository.save(usage);

        int addedMinutes = ticketType.toMinutes(request.getCount());
        int currentApplied = parkingLog.getAppliedMinutes() != null
                ? parkingLog.getAppliedMinutes() : 0;
        parkingLog.updateAppliedMinutes(currentApplied + addedMinutes);

        log.info("티켓 적용 - parkingId: {}, 타입: {}, 수량: {}, 적용분: {}",
                request.getParkingId(), ticketType, request.getCount(), addedMinutes);
    }

    // ─── 티켓 취소 ───────────────────────────────────────────────
    @Override
    public void cancelTicket(TicketCancelRequest request, Long householdId) {
        ParkingLog parkingLog = parkingLogRepository.findByIdWithLock(request.getParkingId())
                .orElseThrow(() -> new EntityNotFoundException("주차 기록을 찾을 수 없습니다."));

        if (parkingLog.getStatus() != ParkingLog.ParkingStatus.PARKED) {
            throw new IllegalStateException("주차 중인 차량이 아닙니다.");
        }

        ParkingTicket.TicketType ticketType =
                ParkingTicket.TicketType.valueOf(request.getTicketType());

        List<TicketUsage> usages = ticketUsageRepository.findByParkingLog(parkingLog);

        Optional<TicketUsage> targetUsage = usages.stream()
                .filter(u -> u.getTicket().getType() == ticketType)
                .findFirst();

        if (targetUsage.isEmpty()) {
            throw new EntityNotFoundException("취소할 티켓 사용 내역이 없습니다.");
        }

        TicketUsage usage = targetUsage.get();
        ParkingTicket ticket = usage.getTicket();

        int cancelMinutes = ticketType.toMinutes(usage.getUsedCount());

        ticket.restoreCount(usage.getUsedCount());

        ticketUsageRepository.delete(usage);

        int currentApplied = parkingLog.getAppliedMinutes() != null
                ? parkingLog.getAppliedMinutes() : 0;
        int newApplied = Math.max(0, currentApplied - cancelMinutes);
        parkingLog.updateAppliedMinutes(newApplied);

        log.info("티켓 취소 - parkingId: {}, 타입: {}, 복구분: {}",
                request.getParkingId(), ticketType, cancelMinutes);
    }
}