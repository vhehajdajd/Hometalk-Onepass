package com.hometalk.onepass.reservation.repository;

import com.hometalk.onepass.reservation.entity.Reservation;
import com.hometalk.onepass.reservation.entity.ReservationTime;
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
        AND r.reservationTime.startTime >= :start 
        AND r.reservationTime.startTime <= :end
        """)
    List<Reservation> findAllByFacilityAndDate(@Param("facilityId") Long facilityId,
                                               @Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);

    // 한 사람이 같은 날짜의 다른 시간대 예약 불가
    @Query("""
        SELECT COUNT(r) > 0 FROM Reservation r 
        WHERE r.user.id = :userId 
        AND r.facility.id = :facilityId
        AND r.reservationTime.startTime >= :start 
        AND r.reservationTime.startTime <= :end
        """)
    boolean existsByUserIdAndFacilityIdAndDate(@Param("userId") Long userId,
                                               @Param("facilityId") Long facilityId,
                                               @Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);


    // 서비스의 findAllWithDetails에서 사용할 메서드
    List<Reservation> findAllByOrderByIdDesc(); // 또는 createdAt 기준

    // 특정 유저의 예약을 최신순으로 가져오는 메서드
    List<Reservation> findByUserIdOrderByIdDesc(Long userId);

    @Query("""
            SELECT r FROM Reservation r
            WHERE r.reservationTime.startTime 
            BETWEEN :start AND :end
            """)
    List<Reservation> findByMonthRange(LocalDateTime start,
                                       LocalDateTime end);
}