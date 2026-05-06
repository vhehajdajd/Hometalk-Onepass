package com.hometalk.onepass.parking.repository;

import com.hometalk.onepass.auth.entity.Household;
import com.hometalk.onepass.parking.entity.ParkingTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingTicketRepository extends JpaRepository<ParkingTicket, Long> {

    // 세대별 티켓 목록 조회
    List<ParkingTicket> findByHousehold(Household household);

    // 세대별 티켓 종류별 조회
    Optional<ParkingTicket> findByHouseholdAndTypeAndIssueYearAndIssueMonth(
            Household household,
            ParkingTicket.TicketType type,
            int issueYear,
            int issueMonth
    );

    // 세대별 해당 월 티켓 전체 조회
    List<ParkingTicket> findByHouseholdAndIssueYearAndIssueMonth(
            Household household,
            int issueYear,
            int issueMonth
    );
}