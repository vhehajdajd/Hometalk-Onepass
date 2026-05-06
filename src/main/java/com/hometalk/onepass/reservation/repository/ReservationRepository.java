package com.hometalk.onepass.reservation.repository;

import com.hometalk.onepass.reservation.entity.Reservation;
import com.hometalk.onepass.reservation.entity.ReservationTime;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // 예약 중복 체크
    // 특정 날짜, 특정 시설의 모든 예약(대기 포함) 조회
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

    // 동일 유저의 동일 시설 중복 예약 여부 확인
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

    // [관리자] 전체 예약 목록 조회
    @Override
    @EntityGraph(attributePaths = {"user", "facility"})
    List<Reservation> findAll();

    // [마이페이지] 특정 유저 예약 내역
    @EntityGraph(attributePaths = {"facility"})
    List<Reservation> findByUserIdOrderByIdDesc(Long userId);

    // 캘린더용
    @Query("""
            SELECT r FROM Reservation r
            WHERE r.reservationTime.startTime 
            BETWEEN :start AND :end
            AND r.status != com.hometalk.onepass.reservation.entity.ReservationStatus.CANCELED
            """)
    List<Reservation> findByMonthRange(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);
}