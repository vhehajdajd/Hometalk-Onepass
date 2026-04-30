package com.hometalk.onepass.parking.service;

import com.hometalk.onepass.parking.dto.response.TicketResponse;
import com.hometalk.onepass.parking.entity.ParkingTicket;

import java.time.LocalDate;
import java.util.List;

public interface TicketService {

    // 매달 1일 전체 세대 티켓 자동 발급
    void issueMonthlyTickets();

    // 관리자 수동 발급
    void issueTicket(Long householdId, ParkingTicket.TicketType type, int totalCount, LocalDate issuedDate);

    // 세대별 전체 티켓 조회
    List<TicketResponse> getHouseholdTickets(Long householdId);

    // 세대별 이번 달 티켓 조회
    List<TicketResponse> getCurrentMonthTickets(Long householdId);
}