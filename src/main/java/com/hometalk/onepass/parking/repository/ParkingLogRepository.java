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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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


    // 주차중 목록용
    @Query("""
        SELECT p FROM ParkingLog p
        LEFT JOIN FETCH p.vehicle v
        WHERE p.status = 'PARKED'
        AND (
            (p.entryType = 'NORMAL' AND v IS NOT NULL AND v.status = 'APPROVED')
            OR p.entryType IN ('RESERVATION', 'MANUAL')
        )
        ORDER BY p.entryTime DESC
    """)
    List<ParkingLog> findValidParkedLogs();


    // 페이지네이션
    @Query(value = "SELECT p FROM ParkingLog p WHERE p.household.id = :householdId " +
            "AND YEAR(p.entryTime) = :year AND MONTH(p.entryTime) = :month " +
            "AND p.deletedAt IS NULL ORDER BY p.entryTime DESC",
            countQuery = "SELECT COUNT(p) FROM ParkingLog p WHERE p.household.id = :householdId " +
                    "AND YEAR(p.entryTime) = :year AND MONTH(p.entryTime) = :month " +
                    "AND p.deletedAt IS NULL")
    Page<ParkingLog> findByHouseholdAndYearAndMonth(
            @Param("householdId") Long householdId,
            @Param("year") int year,
            @Param("month") int month,
            Pageable pageable);

    // Generated Column 사용으로 인덱스 활용
    @Query("""
    SELECT p FROM ParkingLog p
    LEFT JOIN FETCH p.vehicle v
    WHERE p.status = 'PARKED'
      AND p.vehicleNumberLast4 = :last4
      AND (
           (p.entryType = 'NORMAL' AND v IS NOT NULL AND v.status = 'APPROVED')
           OR p.entryType IN ('RESERVATION', 'MANUAL')
      )
      AND p.deletedAt IS NULL
    ORDER BY p.entryTime DESC
    """)
    List<ParkingLog> findParkedByLast4(@Param("last4") String last4);

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



    // 관리자 조회 (수정 완료)
    @Query(value = """
            SELECT p FROM ParkingLog p
            LEFT JOIN FETCH p.household h
            LEFT JOIN p.vehicle v
            WHERE YEAR(p.entryTime) = :year
              AND MONTH(p.entryTime) = :month
              AND p.deletedAt IS NULL

              AND (
                   :type = 'ALL'
                   OR (:type = 'RESIDENT' 
                       AND p.entryType = 'NORMAL'
                       AND v IS NOT NULL
                       AND v.status = 'APPROVED')
                   OR (:type = 'VISITOR'  
                       AND p.entryType IN ('RESERVATION', 'MANUAL'))
              )

              AND (:household IS NULL OR :household = ''
                   OR CONCAT(h.dong, ' ', h.ho) LIKE %:household%)

            ORDER BY p.entryTime DESC
            """,
            countQuery = """
            SELECT COUNT(p) FROM ParkingLog p
            LEFT JOIN p.household h
            LEFT JOIN p.vehicle v
            WHERE YEAR(p.entryTime) = :year
              AND MONTH(p.entryTime) = :month
              AND p.deletedAt IS NULL

              AND (
                   :type = 'ALL'
                   OR (:type = 'RESIDENT' 
                       AND p.entryType = 'NORMAL'
                       AND v IS NOT NULL
                       AND v.status = 'APPROVED')
                   OR (:type = 'VISITOR'  
                       AND p.entryType IN ('RESERVATION', 'MANUAL'))
              )

              AND (:household IS NULL OR :household = ''
                   OR CONCAT(h.dong, ' ', h.ho) LIKE %:household%)
            """)
    Page<ParkingLog> findAllByFilter(
            @Param("year") int year,
            @Param("month") int month,
            @Param("type") String type,
            @Param("household") String household,
            Pageable pageable);


    // Generated Column 사용으로 인덱스 활용
    @Query("""
    SELECT COUNT(p) > 0 FROM ParkingLog p
    WHERE p.status = 'PARKED'
      AND p.deletedAt IS NULL
      AND p.vehicleNumberLast4 = :last4
    """)
    boolean existsParkedByLast4(@Param("last4") String last4);


    // 출차 취소용 (오늘 출차 기록 조회)
    @Query("""
    SELECT p FROM ParkingLog p
    LEFT JOIN FETCH p.vehicle v
    WHERE p.exitTime >= :startOfDay
      AND p.status IN ('EXITED', 'OVERSTAY')
      AND p.deletedAt IS NULL
    ORDER BY p.exitTime DESC
    """)
    List<ParkingLog> findExitedToday(@Param("startOfDay") LocalDateTime startOfDay);

}