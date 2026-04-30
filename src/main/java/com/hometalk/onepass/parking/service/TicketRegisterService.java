package com.hometalk.onepass.parking.service;

import com.hometalk.onepass.parking.dto.request.TicketApplyRequest;
import com.hometalk.onepass.parking.dto.request.TicketCancelRequest;
import com.hometalk.onepass.parking.dto.response.ParkingSearchResponse;

public interface TicketRegisterService {

    // 차량 조회 (주차 시간 + 티켓 현황)
    ParkingSearchResponse searchParkedVehicle(String keyword, Long householdId);

    // 티켓 적용
    void applyTicket(TicketApplyRequest request, Long householdId);

    // 티켓 취소
    void cancelTicket(TicketCancelRequest request, Long householdId);
}