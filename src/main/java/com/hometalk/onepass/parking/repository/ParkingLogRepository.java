package com.hometalk.onepass.parking.repository;

import com.hometalk.onepass.auth.entity.Household;
import com.hometalk.onepass.parking.entity.ParkingLog;
import com.hometalk.onepass.parking.entity.VisitReservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingLogRepository extends JpaRepository<ParkingLog, Long> {

    List<ParkingLog> findByHousehold(Household household);

    List<ParkingLog> findByHouseholdAndEntryTimeBetween(
            Household household,
            LocalDateTime start,
            LocalDateTime end
    );

    List<ParkingLog> findByVehicleNumberEndingWithAndStatus(
            String suffix,
            ParkingLog.ParkingStatus status
    );

    List<ParkingLog> findByStatus(ParkingLog.ParkingStatus status);

    List<ParkingLog> findByEntryTimeBetween(
            LocalDateTime start,
            LocalDateTime end
    );

    List<ParkingLog> findByHouseholdIsNullAndStatus(ParkingLog.ParkingStatus status);

    Optional<ParkingLog> findByVehicleNumberAndStatus(
            String vehicleNumber,
            ParkingLog.ParkingStatus status
    );

    // 퀵서치 - 공백 제거 후 끝 4자리 + PARKED 상태
    @Query("""
        SELECT p FROM ParkingLog p
        WHERE p.status = 'PARKED'
          AND RIGHT(REPLACE(p.vehicleNumber, ' ', ''), 4) = :last4
        ORDER BY p.entryTime DESC
        """)
    List<ParkingLog> findParkedByLast4(@Param("last4") String last4);

    // 출차 처리용 - 비관적 락 적용 (동시 출차 요청 방지)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ParkingLog p WHERE p.parkingId = :id")
    Optional<ParkingLog> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT p FROM ParkingLog p WHERE p.household.id = :householdId " +
            "AND YEAR(p.entryTime) = :year AND MONTH(p.entryTime) = :month " +
            "AND p.deletedAt IS NULL ORDER BY p.entryTime DESC")
    List<ParkingLog> findByHouseholdAndYearAndMonth(
            @Param("householdId") Long householdId,
            @Param("year") int year,
            @Param("month") int month);


    Optional<ParkingLog> findByReservationAndStatus(VisitReservation reservation, ParkingLog.ParkingStatus status);
}