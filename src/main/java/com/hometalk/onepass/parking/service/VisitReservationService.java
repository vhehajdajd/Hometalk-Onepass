package com.hometalk.onepass.parking.service;

import com.hometalk.onepass.parking.dto.request.VisitReservationRequest;
import com.hometalk.onepass.parking.dto.response.VisitReservationResponse;
import com.hometalk.onepass.parking.entity.VisitReservation;

import java.util.List;

public interface VisitReservationService {

    // 방문 예약 등록
    VisitReservationResponse register(Long householdId, VisitReservationRequest request);

    // 방문 예약 단건 조회
    VisitReservationResponse getReservation(Long reservationId);

    // 방문 예약 수정 (소유권 검증 포함)
    // ✅ 보안 수정: householdId 추가 → 내 세대 예약인지 Service에서 검증
    VisitReservationResponse update(Long reservationId, VisitReservationRequest request,
                                    Long householdId);

    // 방문 예약 취소 (소유권 검증 포함)
    // ✅ 보안 수정: householdId 추가 → 내 세대 예약인지 Service에서 검증
    void cancel(Long reservationId, Long householdId);

    // 입차 처리
    void enter(Long reservationId);

    // 세대별 예약 목록 조회
    List<VisitReservationResponse> getHouseholdReservations(Long householdId);

    // 상태별 조회
    List<VisitReservationResponse> getHouseholdReservationsByStatus(
            Long householdId,
            VisitReservation.ReservationStatus status
    );

    // 수동 입차 목록 조회 (PENDING_CONFIRM)
    List<VisitReservationResponse> getPendingConfirmReservations(Long householdId);
}