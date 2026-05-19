package com.hometalk.onepass.parking.dto.response;

import com.hometalk.onepass.parking.entity.ParkingLog;
import com.hometalk.onepass.parking.entity.ParkingTicket;
import com.hometalk.onepass.parking.entity.TicketUsage;
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
    private final String neededTime;
    private final int dayRemaining;
    private final int hourRemaining;
    private final boolean unregistered;
    private final boolean manualMatched;  // 수동 입차 + 세대 매칭 완료 여부
    private final boolean dayApplied;
    private final boolean hourApplied;
    private final int dayAppliedCount;
    private final int hourAppliedCount;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public ParkingSearchResponse(ParkingLog log, List<ParkingTicket> tickets, List<TicketUsage> usages) {
        this.parkingId = log.getParkingId();
        this.vehicleNumber = log.getVehicleNumber();
        this.entryTime = log.getEntryTime().format(FMT);

        long totalMinutes = Duration.between(log.getEntryTime(), LocalDateTime.now()).toMinutes();
        this.parkingTime = formatMinutes(totalMinutes);
        this.totalTime = formatMinutes(totalMinutes);

        int applied = log.getAppliedMinutes() != null ? log.getAppliedMinutes() : 0;
        this.appliedTime = formatMinutes(applied);

        long remaining = Math.max(0, applied - totalMinutes);
        this.remainingTime = formatMinutes(remaining);

        long needed = Math.max(0, totalMinutes - applied);
        this.neededTime = needed > 0 ? formatMinutes(needed) : null;

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

        this.unregistered = log.getHousehold() == null;

        // 수동 입차 + 세대 매칭 완료된 경우 → 내 손님 해제 버튼 표시용
        this.manualMatched = log.getEntryType() == ParkingLog.EntryType.MANUAL
                && log.getHousehold() != null;

        this.dayApplied = usages.stream()
                .anyMatch(u -> u.getTicket().getType() == ParkingTicket.TicketType.DAY);
        this.hourApplied = usages.stream()
                .anyMatch(u -> u.getTicket().getType() == ParkingTicket.TicketType.HOUR);

        this.dayAppliedCount = usages.stream()
                .filter(u -> u.getTicket().getType() == ParkingTicket.TicketType.DAY)
                .mapToInt(TicketUsage::getUsedCount)
                .sum();
        this.hourAppliedCount = usages.stream()
                .filter(u -> u.getTicket().getType() == ParkingTicket.TicketType.HOUR)
                .mapToInt(TicketUsage::getUsedCount)
                .sum();
    }

    private String formatMinutes(long minutes) {
        if (minutes >= 60) {
            return (minutes / 60) + "시간 " + (minutes % 60) + "분";
        }
        return minutes + "분";
    }
}