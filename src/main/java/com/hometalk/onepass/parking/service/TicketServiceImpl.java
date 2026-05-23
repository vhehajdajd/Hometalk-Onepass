package com.hometalk.onepass.parking.service;

import com.hometalk.onepass.auth.entity.Household;
import com.hometalk.onepass.auth.repository.HouseholdRepository;
import com.hometalk.onepass.parking.dto.response.TicketResponse;
import com.hometalk.onepass.parking.entity.ParkingTicket;
import com.hometalk.onepass.parking.exception.ParkingException;
import com.hometalk.onepass.parking.repository.ParkingTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TicketServiceImpl implements TicketService {

    private final ParkingTicketRepository parkingTicketRepository;
    private final HouseholdRepository householdRepository;

    private static final int HOUR_TICKET_COUNT = 60;
    private static final int DAY_TICKET_COUNT = 6;

    @Override
    public void issueMonthlyTickets() {
        LocalDate today = LocalDate.now();
        List<Household> households = householdRepository.findAll();
        for (Household household : households) {
            issueIfNotExists(household, ParkingTicket.TicketType.HOUR, HOUR_TICKET_COUNT, today);
            issueIfNotExists(household, ParkingTicket.TicketType.DAY, DAY_TICKET_COUNT, today);
        }
        log.info("{}년 {}월 티켓 발급 완료 - 총 {}세대",
                today.getYear(), today.getMonthValue(), households.size());
    }

    @Override
    public void issueTicket(Long householdId, ParkingTicket.TicketType type,
                            int totalCount, LocalDate issuedDate) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new ParkingException("세대를 찾을 수 없습니다."));

        Optional<ParkingTicket> existing = parkingTicketRepository
                .findByHouseholdAndTypeAndIssueYearAndIssueMonth(
                        household, type, issuedDate.getYear(), issuedDate.getMonthValue());

        if (existing.isPresent()) {
            existing.get().addCount(totalCount);
            log.info("티켓 수량 추가 - 세대: {}, 타입: {}, 추가수량: {}", householdId, type, totalCount);
        } else {
            parkingTicketRepository.save(new ParkingTicket(household, type, totalCount, issuedDate));
            log.info("티켓 신규 발급 - 세대: {}, 타입: {}, 수량: {}", householdId, type, totalCount);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> getHouseholdTickets(Long householdId) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new ParkingException("세대를 찾을 수 없습니다."));
        return parkingTicketRepository.findByHousehold(household)
                .stream().map(TicketResponse::new).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> getCurrentMonthTickets(Long householdId) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new ParkingException("세대를 찾을 수 없습니다."));
        LocalDate today = LocalDate.now();
        return parkingTicketRepository
                .findByHouseholdAndIssueYearAndIssueMonth(
                        household, today.getYear(), today.getMonthValue())
                .stream().map(TicketResponse::new).collect(Collectors.toList());
    }

    private void issueIfNotExists(Household household, ParkingTicket.TicketType type,
                                  int count, LocalDate issuedDate) {
        try {
            boolean exists = parkingTicketRepository
                    .findByHouseholdAndTypeAndIssueYearAndIssueMonth(
                            household, type, issuedDate.getYear(), issuedDate.getMonthValue())
                    .isPresent();
            if (!exists) {
                parkingTicketRepository.save(new ParkingTicket(household, type, count, issuedDate));
            }
        } catch (DataIntegrityViolationException e) {
            log.warn("티켓 중복 발급 충돌 스킵 - 세대: {}, 타입: {}, {}년 {}월",
                    household.getId(), type, issuedDate.getYear(), issuedDate.getMonthValue());
        }
    }

    @Override
    public void issueBulkTickets(List<Long> householdIds, ParkingTicket.TicketType type,
                                 int totalCount, LocalDate issuedDate) {
        for (Long householdId : householdIds) {
            issueTicket(householdId, type, totalCount, issuedDate); // 기존 메서드 재사용
        }
        log.info("bulk 티켓 발급 완료 - {}개 세대, 타입: {}, 수량: {}", householdIds.size(), type, totalCount);
    }
}