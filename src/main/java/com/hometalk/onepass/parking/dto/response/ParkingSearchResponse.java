package com.hometalk.onepass.parking.dto.response;

import com.hometalk.onepass.parking.entity.ParkingLog;
import com.hometalk.onepass.parking.entity.ParkingTicket;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter
public class ParkingSearchResponse {

    private final Long parkingId;
    private final String vehicleNumber;
    private final String entryTime;
    private final String parkingTime;
    private final String totalTime;
    private final String appliedTime;
    private final String remainingTime;
    private final int dayRemaining;
    private final int hourRemaining;
    private final boolean unregistered;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public ParkingSearchResponse(ParkingLog log, List<ParkingTicket> tickets) {
        this.parkingId = log.getParkingId();
        this.vehicleNumber = log.getVehicleNumber();
        this.entryTime = log.getEntryTime().format(FMT);

        // 주차 시간 계산
        long totalMinutes = Duration.between(log.getEntryTime(), LocalDateTime.now()).toMinutes();
        this.parkingTime = formatMinutes(totalMinutes);
        this.totalTime = formatMinutes(totalMinutes);

        // 현재까지 적용된 티켓 시간
        int applied = log.getAppliedMinutes() != null ? log.getAppliedMinutes() : 0;
        this.appliedTime = formatMinutes(applied);

        // 남은 시간 (음수면 0)
        long remaining = Math.max(0, applied - totalMinutes);
        this.remainingTime = formatMinutes(remaining);

        // 티켓 잔여 수량
        int day = 0;
        int hour = 0;
        for (ParkingTicket ticket : tickets) {
            if (ticket.getType() == ParkingTicket.TicketType.DAY) {
                day = ticket.getRemainingCount();
            } else if (ticket.getType() == ParkingTicket.TicketType.HOUR) {
                hour = ticket.getRemainingCount();
            }
        }
        this.dayRemaining = day;
        this.hourRemaining = hour;

        // 미등록 차량 여부 (household 없으면 미등록)
        this.unregistered = log.getHousehold() == null;
    }

    private String formatMinutes(long minutes) {
        if (minutes >= 60) {
            return (minutes / 60) + "시간 " + (minutes % 60) + "분";
        }
        return minutes + "분";
    }
}