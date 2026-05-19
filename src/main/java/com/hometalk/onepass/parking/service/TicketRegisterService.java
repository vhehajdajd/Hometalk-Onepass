package com.hometalk.onepass.parking.service;

import com.hometalk.onepass.parking.dto.request.TicketApplyRequest;
import com.hometalk.onepass.parking.dto.request.TicketCancelRequest;
import com.hometalk.onepass.parking.dto.response.ParkingSearchResponse;
import java.util.List;

public interface TicketRegisterService {

    // 차량 조회 (단건)
    ParkingSearchResponse searchParkedVehicle(String keyword, Long householdId);

    // 차량 조회 (다건 - 차량 선택용)
    List<ParkingSearchResponse> searchParkedVehicleList(String keyword, Long householdId);

    // 티켓 적용
    void applyTicket(TicketApplyRequest request, Long householdId);

    // 티켓 취소
    void cancelTicket(TicketCancelRequest request, Long householdId);

    // 내 세대 주차 차량 목록
    List<ParkingSearchResponse> getMyParkedVehicles(Long householdId);

    // 내 손님 해제
    void unmatchHousehold(Long parkingId, Long householdId);
}