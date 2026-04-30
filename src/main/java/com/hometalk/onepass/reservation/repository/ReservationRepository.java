package com.hometalk.onepass.reservation.repository;

import com.hometalk.onepass.reservation.entity.Reservation;
import com.hometalk.onepass.reservation.entity.ReservationTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // 본인 중복 예약 체크
    boolean existsByFacilityIdAndUserId(Long facilityId, Long userId);

    // 시간대 중복 예약 체크
    boolean existsByFacilityIdAndReservationTime(Long facilityId, ReservationTime reservationTime);

    // 서비스의 findAllWithDetails에서 사용할 메서드
    List<Reservation> findAllByOrderByIdDesc(); // 또는 createdAt 기준
}