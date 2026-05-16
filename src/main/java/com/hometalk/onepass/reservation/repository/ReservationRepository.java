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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // 전체 조회
    @Override
    @EntityGraph(attributePaths = {"user", "facility"})
    Page<Reservation> findAll(Pageable pageable);

    // 내 예약 내역
    @EntityGraph(attributePaths = {"facility"})
    @Query("""
            SELECT r FROM Reservation r
            WHERE r.user.id = :userId
            AND (:status IS NULL OR r.status = :status)
            ORDER BY 
                CASE WHEN r.reservationTime.endTime < :now THEN 1 ELSE 0 END ASC
            """)
    Page<Reservation> findByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") ReservationStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    // 시설별 월간 예약 조회
    @EntityGraph(attributePaths = {"user", "facility"})
    @Query("""
            SELECT r FROM Reservation r
            WHERE r.facility.id = :facilityId
            AND r.reservationTime.startTime BETWEEN :start AND :end
            AND r.status NOT IN (
                com.hometalk.onepass.reservation.entity.ReservationStatus.CANCELED,
                com.hometalk.onepass.reservation.entity.ReservationStatus.REJECTED
            )
            """)
    List<Reservation> findByFacilityIdAndMonthRange(
            @Param("facilityId") Long facilityId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // 내 최근 예약
    @EntityGraph(attributePaths = {"facility"})
    List<Reservation> findTop5ByUser_IdOrderByIdDesc(Long userId);

    // 전체 최신순
    @EntityGraph(attributePaths = {"user", "facility"})
    List<Reservation> findTop10ByOrderByIdDesc();

    // 종료 시간이 지났지만 아직 CONFIRMED 상태인 예약 조회
    @EntityGraph(attributePaths = {"facility", "user"})
    List<Reservation> findByStatusAndReservationTime_EndTimeBefore(ReservationStatus status, LocalDateTime dateTime);

    // 예약 시작 알림 대상 조회
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

    // 상태 + 종료 시간 기준 일괄 삭제
    @Modifying
    @Transactional
    @Query("DELETE FROM Reservation r WHERE r.status = :status AND r.reservationTime.endTime < :dateTime")
    void bulkDeleteByStatusAndEndTime(@Param("status") ReservationStatus status, @Param("dateTime") LocalDateTime dateTime);

    // 상태별 예약 개수
    long countByStatus(ReservationStatus status);

    // 오늘 전체 예약 수
    @Query("SELECT COUNT(r) FROM Reservation r " +
            "WHERE r.reservationTime.startTime BETWEEN :start AND :end")
    long countTodayReservations(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 현재 이용 중인 예약 수
    @Query("SELECT COUNT(r) FROM Reservation r " +
            "WHERE r.status = 'CONFIRMED' " +
            "AND :now BETWEEN r.reservationTime.startTime AND r.reservationTime.endTime")
    long countActiveReservations(@Param("now") LocalDateTime now);

    // 상태 필터 포함 전체 조회
    @Query("SELECT r FROM Reservation r " +
            "WHERE (:status IS NULL OR r.status = :status) " +
            "ORDER BY r.id DESC")
    Page<Reservation> findAllByStatus(@Param("status") ReservationStatus status, Pageable pageable);


    // =========================
    // 중복 예약 방지
    // =========================

    // 동일 시간대 타 시설 예약 여부 확인
    @Query("""
            SELECT COUNT(r) > 0 FROM Reservation r
            WHERE r.user.id = :userId
            AND r.status IN (
                com.hometalk.onepass.reservation.entity.ReservationStatus.CONFIRMED,
                com.hometalk.onepass.reservation.entity.ReservationStatus.PENDING
            )
            AND r.reservationTime.startTime < :end
            AND r.reservationTime.endTime > :start
            """)
    boolean existsOverlapReservation(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // 동일 시설 활성 예약 여부 확인
    @Query("""
            SELECT COUNT(r) > 0 FROM Reservation r
            WHERE r.user.id = :userId
            AND r.facility.id = :facilityId
            AND r.status IN (
                com.hometalk.onepass.reservation.entity.ReservationStatus.CONFIRMED,
                com.hometalk.onepass.reservation.entity.ReservationStatus.PENDING
            )
            """)
    boolean existsActiveReservation(
            @Param("userId") Long userId,
            @Param("facilityId") Long facilityId
    );

    // 특정 시간대 현재 예약 인원 수
    @Query("""
            SELECT COUNT(r) FROM Reservation r
            WHERE r.facility.id = :facilityId
            AND r.reservationTime.startTime < :end
            AND r.reservationTime.endTime > :start
            AND r.status IN :activeStatuses
            """)
    long countCurrentReservations(
            @Param("facilityId") Long facilityId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("activeStatuses") Collection<ReservationStatus> activeStatuses
    );

    // 시설별 날짜 예약 조회
    @Query("""
            SELECT r FROM Reservation r
            WHERE r.facility.id = :facilityId
            AND FUNCTION('DATE', r.reservationTime.startTime) = :reservationDate
            AND r.status NOT IN (
                com.hometalk.onepass.reservation.entity.ReservationStatus.CANCELED,
                com.hometalk.onepass.reservation.entity.ReservationStatus.REJECTED
            )
            """)
    List<Reservation> findByFacilityIdAndReservationDate(
            @Param("facilityId") Long facilityId,
            @Param("reservationDate") LocalDate reservationDate
    );

    // 사용자 날짜별 예약 조회
    @Query("""
            SELECT r FROM Reservation r
            WHERE r.user.id = :userId
            AND FUNCTION('DATE', r.reservationTime.startTime) = :reservationDate
            AND r.status NOT IN (
                com.hometalk.onepass.reservation.entity.ReservationStatus.CANCELED,
                com.hometalk.onepass.reservation.entity.ReservationStatus.REJECTED
            )
            """)
    List<Reservation> findByUserIdAndReservationDate(
            @Param("userId") Long userId,
            @Param("reservationDate") LocalDate reservationDate
    );
}