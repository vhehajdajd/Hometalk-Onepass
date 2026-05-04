package com.hometalk.onepass.parking.repository;

import com.hometalk.onepass.parking.entity.ParkingLog;
import com.hometalk.onepass.parking.entity.ParkingTicket;
import com.hometalk.onepass.parking.entity.TicketUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketUsageRepository extends JpaRepository<TicketUsage, Long> {

    // 주차 기록별 티켓 사용 내역 조회
    List<TicketUsage> findByParkingLog(ParkingLog parkingLog);

    // 티켓별 사용 내역 조회
    List<TicketUsage> findByTicket(ParkingTicket ticket);

    // 주차 기록 ID로 사용된 총 시간 계산
    List<TicketUsage> findByParkingLogParkingId(Long parkingId);
}