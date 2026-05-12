package com.hometalk.onepass.reservation.repository;

import com.hometalk.onepass.reservation.entity.Reservation;
import com.hometalk.onepass.reservation.entity.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

    // 전체 조회
    @Override
    @EntityGraph(attributePaths = {"user", "facility"})
    Page<Reservation> findAll(Pageable pageable);

    // 내 예약 내역
    @EntityGraph(attributePaths = {"facility"})
    Page<Reservation> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

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

    // 전체 최신순
    @EntityGraph(attributePaths = {"user", "facility"})
    List<Reservation> findTop10ByOrderByIdDesc();

    // 종료 시간이 지났지만 아직 'CONFIRMED'인 예약을 'FINISHED'로 변경
    @EntityGraph(attributePaths = {"facility", "user"})
    List<Reservation> findByStatusAndReservationTime_EndTimeBefore(ReservationStatus status, LocalDateTime dateTime);

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

    @Modifying
    @Transactional
    @Query("DELETE FROM Reservation r WHERE r.status = :status AND r.reservationTime.endTime < :dateTime")
    void bulkDeleteByStatusAndEndTime(@Param("status") ReservationStatus status, @Param("dateTime") LocalDateTime dateTime);

    // 1. 상태별 예약 개수 (승인대기 건수 등 조회용)
    long countByStatus(ReservationStatus status);

    // 2. 오늘 날짜의 전체 예약 개수
    @Query("SELECT COUNT(r) FROM Reservation r " +
            "WHERE r.reservationTime.startTime BETWEEN :start AND :end")
    long countTodayReservations(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 3. 현재 실제로 시설을 이용 중인 건수 (상태가 CONFIRMED이고 현재 시간이 시작~종료 사이인 경우)
    @Query("SELECT COUNT(r) FROM Reservation r " +
            "WHERE r.status = 'CONFIRMED' " +
            "AND :now BETWEEN r.reservationTime.startTime AND r.reservationTime.endTime")
    long countActiveReservations(@Param("now") LocalDateTime now);

    // 상태 필터가 있을 때와 없을 때를 모두 처리하는 쿼리
    @Query("SELECT r FROM Reservation r " +
            "WHERE (:status IS NULL OR r.status = :status) " +
            "ORDER BY r.id DESC")
    Page<Reservation> findAllByStatus(@Param("status") ReservationStatus status, Pageable pageable);
}