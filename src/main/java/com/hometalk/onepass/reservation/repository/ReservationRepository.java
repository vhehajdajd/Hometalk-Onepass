package com.hometalk.onepass.reservation.repository;

import com.hometalk.onepass.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
        SELECT r FROM Reservation r 
        WHERE r.facility.id = :facilityId 
        AND r.status != com.hometalk.onepass.reservation.entity.ReservationStatus.CANCELED
        AND r.reservationTime.startTime < :end 
        AND r.reservationTime.endTime > :start
        """)
    List<Reservation> findConflictingReservations(@Param("facilityId") Long facilityId,
                                                  @Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end);

    @Query("""
        SELECT COUNT(r) > 0 FROM Reservation r 
        WHERE r.user.id = :userId 
        AND r.facility.id = :facilityId
        AND r.status != com.hometalk.onepass.reservation.entity.ReservationStatus.CANCELED
        AND r.reservationTime.startTime < :end 
        AND r.reservationTime.endTime > :start
        """)
    boolean existsByUserIdAndOverlapTime(@Param("userId") Long userId,
                                         @Param("facilityId") Long facilityId,
                                         @Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    @Override
    @EntityGraph(attributePaths = {"user", "facility"})
    List<Reservation> findAll();

    @EntityGraph(attributePaths = {"facility"})
    List<Reservation> findByUserIdOrderByIdDesc(Long userId);

    @EntityGraph(attributePaths = {"user", "facility"})
    @Query("""
            SELECT r FROM Reservation r
            WHERE r.facility.id = :facilityId
            AND r.reservationTime.startTime BETWEEN :start AND :end
            AND r.status != com.hometalk.onepass.reservation.entity.ReservationStatus.CANCELED
            """)
    List<Reservation> findByFacilityIdAndMonthRange(
            @Param("facilityId") Long facilityId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @EntityGraph(attributePaths = {"facility"})
    List<Reservation> findTop5ByUser_IdOrderByIdDesc(Long userId);

    @EntityGraph(attributePaths = {"user", "facility"})
    List<Reservation> findTop10ByOrderByIdDesc();

    @EntityGraph(attributePaths = {"user", "facility"})
    @Query("""
        SELECT r FROM Reservation r
        WHERE r.status = com.hometalk.onepass.reservation.entity.ReservationStatus.CONFIRMED
        AND r.reservationTime.startTime BETWEEN :from AND :to
        """)
    List<Reservation> findConfirmedReservationsStartingBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    void deleteByReservationTime_EndTimeBefore(LocalDateTime cutoffDateTime);
}