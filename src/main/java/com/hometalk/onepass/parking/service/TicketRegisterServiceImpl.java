package com.hometalk.onepass.parking.service;

import com.hometalk.onepass.auth.entity.Household;
import com.hometalk.onepass.auth.repository.HouseholdRepository;
import com.hometalk.onepass.parking.dto.request.TicketApplyRequest;
import com.hometalk.onepass.parking.dto.request.TicketCancelRequest;
import com.hometalk.onepass.parking.dto.response.ParkingSearchResponse;
import com.hometalk.onepass.parking.entity.ParkingLog;
import com.hometalk.onepass.parking.entity.ParkingTicket;
import com.hometalk.onepass.parking.entity.TicketUsage;
import com.hometalk.onepass.parking.exception.ParkingException;
import com.hometalk.onepass.parking.repository.ParkingLogRepository;
import com.hometalk.onepass.parking.repository.ParkingTicketRepository;
import com.hometalk.onepass.parking.repository.TicketUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TicketRegisterServiceImpl implements TicketRegisterService {

    private final ParkingLogRepository parkingLogRepository;
    private final ParkingTicketRepository parkingTicketRepository;
    private final TicketUsageRepository ticketUsageRepository;
    private final HouseholdRepository householdRepository;

    @Override
    @Transactional(readOnly = true)
    public ParkingSearchResponse searchParkedVehicle(String keyword, Long householdId) {
        String normalized = keyword.replace(" ", "");
        if (normalized.length() < 1) throw new ParkingException("차량 번호를 입력해주세요.");
        String last4 = normalized.length() >= 4 ? normalized.substring(normalized.length() - 4) : normalized;

        List<ParkingLog> logs = parkingLogRepository.findParkedByLast4(last4);
        if (logs.isEmpty()) throw new ParkingException("주차 중인 차량이 없습니다.");

        ParkingLog parkingLog = logs.get(0);
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new ParkingException("세대를 찾을 수 없습니다."));

        LocalDate today = LocalDate.now();
        List<ParkingTicket> tickets = parkingTicketRepository
                .findByHouseholdAndIssueYearAndIssueMonth(
                        household, today.getYear(), today.getMonthValue());
        List<TicketUsage> usages = ticketUsageRepository.findByParkingLog(parkingLog);

        return new ParkingSearchResponse(parkingLog, tickets, usages);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSearchResponse> searchParkedVehicleList(String keyword, Long householdId) {
        String normalized = keyword.replace(" ", "");
        if (normalized.length() < 1) throw new ParkingException("차량 번호를 입력해주세요.");
        String last4 = normalized.length() >= 4 ? normalized.substring(normalized.length() - 4) : normalized;

        List<ParkingLog> logs = parkingLogRepository.findParkedByLast4(last4);
        if (logs.isEmpty()) throw new ParkingException("주차 중인 차량이 없습니다.");

        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new ParkingException("세대를 찾을 수 없습니다."));

        LocalDate today = LocalDate.now();
        List<ParkingTicket> tickets = parkingTicketRepository
                .findByHouseholdAndIssueYearAndIssueMonth(
                        household, today.getYear(), today.getMonthValue());

        return logs.stream()
                .filter(log -> {
                    if (log.getEntryType() == ParkingLog.EntryType.NORMAL) return false;
                    if (log.getEntryType() == ParkingLog.EntryType.MANUAL) return true;
                    return log.getHousehold() != null &&
                            log.getHousehold().getId().equals(householdId);
                })
                .map(log -> new ParkingSearchResponse(
                        log, tickets, ticketUsageRepository.findByParkingLog(log)))
                .collect(Collectors.toList());
    }

    @Override
    public void applyTicket(TicketApplyRequest request, Long householdId) {
        ParkingLog parkingLog = parkingLogRepository.findByIdWithLock(request.getParkingId())
                .orElseThrow(() -> new ParkingException("주차 기록을 찾을 수 없습니다."));

        if (parkingLog.getStatus() != ParkingLog.ParkingStatus.PARKED) {
            throw new ParkingException("주차 중인 차량이 아닙니다.");
        }

        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new ParkingException("세대를 찾을 수 없습니다."));

        ParkingTicket.TicketType ticketType =
                ParkingTicket.TicketType.valueOf(request.getTicketType());
        LocalDate today = LocalDate.now();

        ParkingTicket ticket = parkingTicketRepository
                .findByHouseholdAndTypeAndIssueYearAndIssueMonth(
                        household, ticketType, today.getYear(), today.getMonthValue())
                .orElseThrow(() -> new ParkingException("보유한 티켓이 없습니다."));

        if (!ticket.isEnough(request.getCount())) {
            throw new ParkingException("티켓 잔여 수량이 부족합니다.");
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

    @Override
    public void cancelTicket(TicketCancelRequest request, Long householdId) {
        ParkingLog parkingLog = parkingLogRepository.findByIdWithLock(request.getParkingId())
                .orElseThrow(() -> new ParkingException("주차 기록을 찾을 수 없습니다."));

        if (parkingLog.getStatus() != ParkingLog.ParkingStatus.PARKED) {
            throw new ParkingException("주차 중인 차량이 아닙니다.");
        }

        ParkingTicket.TicketType ticketType =
                ParkingTicket.TicketType.valueOf(request.getTicketType());

        List<TicketUsage> targetUsages = ticketUsageRepository.findByParkingLog(parkingLog)
                .stream()
                .filter(u -> u.getTicket().getType() == ticketType)
                .collect(Collectors.toList());

        if (targetUsages.isEmpty()) {
            throw new ParkingException("취소할 티켓 사용 내역이 없습니다.");
        }

        int remainCount = request.getCount();
        int totalCancelMinutes = 0;

        for (TicketUsage usage : targetUsages) {
            if (remainCount <= 0) break;
            if (usage.getUsedCount() <= remainCount) {
                remainCount -= usage.getUsedCount();
                totalCancelMinutes += ticketType.toMinutes(usage.getUsedCount());
                usage.getTicket().restoreCount(usage.getUsedCount());
                ticketUsageRepository.delete(usage);
            } else {
                totalCancelMinutes += ticketType.toMinutes(remainCount);
                usage.getTicket().restoreCount(remainCount);
                usage.updateCount(usage.getUsedCount() - remainCount);
                remainCount = 0;
            }
        }

        int currentApplied = parkingLog.getAppliedMinutes() != null
                ? parkingLog.getAppliedMinutes() : 0;
        parkingLog.updateAppliedMinutes(Math.max(0, currentApplied - totalCancelMinutes));

        log.info("티켓 취소 - parkingId: {}, 타입: {}, 복구분: {}",
                request.getParkingId(), ticketType, totalCancelMinutes);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSearchResponse> getMyParkedVehicles(Long householdId) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new ParkingException("세대를 찾을 수 없습니다."));

        LocalDate today = LocalDate.now();
        List<ParkingTicket> tickets = parkingTicketRepository
                .findByHouseholdAndIssueYearAndIssueMonth(
                        household, today.getYear(), today.getMonthValue());

        List<ParkingLog> logs = parkingLogRepository.findByStatus(ParkingLog.ParkingStatus.PARKED);

        return logs.stream()
                .filter(log -> log.getEntryType() == ParkingLog.EntryType.RESERVATION
                        || log.getEntryType() == ParkingLog.EntryType.MANUAL)
                .filter(log -> log.getHousehold() != null
                        && log.getHousehold().getId().equals(householdId))
                .map(log -> new ParkingSearchResponse(
                        log, tickets, ticketUsageRepository.findByParkingLog(log)))
                .collect(Collectors.toList());
    }

    @Override
    public void unmatchHousehold(Long parkingId, Long householdId) {
        ParkingLog parkingLog = parkingLogRepository.findByIdWithLock(parkingId)
                .orElseThrow(() -> new ParkingException("주차 기록을 찾을 수 없습니다."));

        if (parkingLog.getEntryType() != ParkingLog.EntryType.MANUAL) {
            throw new ParkingException("수동 입차 차량만 해제할 수 있습니다.");
        }
        if (parkingLog.getHousehold() == null
                || !parkingLog.getHousehold().getId().equals(householdId)) {
            throw new ParkingException("내 손님으로 등록된 차량이 아닙니다.");
        }

        // 티켓 사용 내역 전체 복구
        List<TicketUsage> usages = ticketUsageRepository.findByParkingLog(parkingLog);
        for (TicketUsage usage : usages) {
            usage.getTicket().restoreCount(usage.getUsedCount());
            ticketUsageRepository.delete(usage);
        }

        // appliedMinutes 초기화 + ParkingLog household 해제
        parkingLog.updateAppliedMinutes(0);
        parkingLog.unmatchHousehold();

        // VisitReservation household도 해제 → PENDING_CONFIRM으로 되돌리기
        if (parkingLog.getReservation() != null) {
            parkingLog.getReservation().unmatchHousehold();
        }

        log.info("내 손님 해제 - parkingId: {}, householdId: {}", parkingId, householdId);
    }
}