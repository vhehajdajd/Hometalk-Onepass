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

    // 본인 중복 예약 체크
    boolean existsByFacilityIdAndUserId(Long facilityId, Long userId);

    // 시간대 중복 예약 체크
    boolean existsByFacilityIdAndReservationTime(Long facilityId, ReservationTime reservationTime);

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