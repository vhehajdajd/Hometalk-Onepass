package com.hometalk.onepass.parking.repository;

import com.hometalk.onepass.auth.entity.Household;
import com.hometalk.onepass.parking.entity.VisitReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VisitReservationRepository extends JpaRepository<VisitReservation, Long> {

    List<VisitReservation> findByHousehold(Household household);

    List<VisitReservation> findByHousehold_Id(Long householdId);

    List<VisitReservation> findByHouseholdAndStatus(Household household, VisitReservation.ReservationStatus status);

    List<VisitReservation> findByHousehold_IdAndStatus(Long householdId, VisitReservation.ReservationStatus status);

    List<VisitReservation> findByVehicleNumberEndingWithAndStatus(String suffix, VisitReservation.ReservationStatus status);

    List<VisitReservation> findByReservedAtBetweenAndStatus(LocalDateTime start, LocalDateTime end, VisitReservation.ReservationStatus status);

    boolean existsByVehicleNumberAndReservedAt(String vehicleNumber, LocalDateTime reservedAt);

    // 퀵서치 - 공백 제거 후 끝 4자리 비교 (RESERVED만)
    @Query("""
        SELECT r FROM VisitReservation r
        JOIN FETCH r.household h
        WHERE r.status = 'RESERVED'
          AND r.deletedAt IS NULL
          AND CAST(r.reservedAt AS date) = :today
          AND RIGHT(REPLACE(r.vehicleNumber, ' ', ''), 4) = :last4
        """)
    List<VisitReservation> findTodayReservedByLast4(
            @Param("last4") String last4,
            @Param("today") LocalDate today
    );

    // 오늘 방문 예정 목록 전체
    @Query("""
        SELECT r FROM VisitReservation r
        JOIN FETCH r.household h
        WHERE r.status = 'RESERVED'
          AND r.deletedAt IS NULL
          AND CAST(r.reservedAt AS date) = :today
        ORDER BY r.reservedAt ASC
        """)
    List<VisitReservation> findTodayReserved(@Param("today") LocalDate today);

    // 미등록 차량 검색 - household null + PENDING_CONFIRM + 끝 4자리
    @Query("""
        SELECT r FROM VisitReservation r
        WHERE r.status = 'PENDING_CONFIRM'
          AND r.household IS NULL
          AND r.deletedAt IS NULL
          AND RIGHT(REPLACE(r.vehicleNumber, ' ', ''), 4) = :last4
        """)
    List<VisitReservation> findUnregisteredByLast4(@Param("last4") String last4);

    // 세대별 예약 목록 조회 (노출 조건 적용)
    // RESERVED: 항상 노출
    // ENTERED: 당일만 노출
    // CANCELLED: 7일간 노출
    @Query("""
        SELECT r FROM VisitReservation r
        WHERE r.household.id = :householdId
          AND r.deletedAt IS NULL
          AND r.status != 'PENDING_CONFIRM'
          AND (
              r.status = 'RESERVED'
              OR (r.status = 'ENTERED' AND CAST(r.reservedAt AS date) = :today)
              OR (r.status = 'CANCELLED' AND r.reservedAt >= :sevenDaysAgo)
          )
        ORDER BY
            CASE r.status WHEN 'RESERVED' THEN 0 ELSE 1 END ASC,
            r.reservedAt DESC
        """)
    List<VisitReservation> findHouseholdReservationsFiltered(
            @Param("householdId") Long householdId,
            @Param("today") LocalDate today,
            @Param("sevenDaysAgo") LocalDateTime sevenDaysAgo
    );
}