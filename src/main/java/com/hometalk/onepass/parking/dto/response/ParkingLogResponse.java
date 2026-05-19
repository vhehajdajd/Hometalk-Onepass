package com.hometalk.onepass.parking.dto.response;

import com.hometalk.onepass.parking.entity.ParkingLog;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
public class ParkingLogResponse {

    private final Long parkingId;
    private final String vehicleNumber;
    private final String household;
    private final boolean householdConfirmed;
    private final String entryType;
    private final String entryTime;
    private final String exitTime;
    private final String status;
    private final String purpose;
    private final String reservedAt;
    private final String userName;
    private final String parkingTime;
    private final String ticketInfo;
    private final boolean canExit;
    private final boolean canCancelExit;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("MM.dd HH:mm");

    public ParkingLogResponse(ParkingLog log, int availableMinutes) {
        this.parkingId = log.getParkingId();
        this.vehicleNumber = log.getVehicleNumber();
        this.householdConfirmed = log.getHousehold() != null;
        this.household = this.householdConfirmed
                ? log.getHousehold().getDong() + " " + log.getHousehold().getHo()
                : "세대 미확인";
        this.entryType = log.getEntryType().name();
        this.entryTime = log.getEntryTime().format(FMT);
        this.exitTime = log.getExitTime() != null
                ? log.getExitTime().format(FMT)
                : null;
        this.status = log.getStatus().name();
        this.purpose = log.getReservation() != null
                ? log.getReservation().getPurpose()
                : null;
        this.reservedAt = log.getReservation() != null
                && log.getReservation().getReservedAt() != null
                ? log.getReservation().getReservedAt().format(FMT)
                : null;

        // soft delete된 차량 방어 처리
        String tempUserName = null;
        try {
            if (log.getVehicle() != null && log.getVehicle().getUser() != null) {
                tempUserName = log.getVehicle().getUser().getName();
            }
        } catch (Exception e) {
            // soft delete된 차량은 무시
        }
        this.userName = tempUserName;

        // 주차 시간 계산
        long totalMinutes = Duration.between(
                log.getEntryTime(), LocalDateTime.now()).toMinutes();
        this.parkingTime = formatMinutes(totalMinutes);

        // 티켓 정보
        if (log.getEntryType() == ParkingLog.EntryType.NORMAL) {
            this.ticketInfo = "해당 없음";
        } else {
            this.ticketInfo = availableMinutes > 0
                    ? formatMinutes(availableMinutes) + " 사용 가능"
                    : "티켓 없음";
        }

        // 출차 가능 여부
        if (!this.householdConfirmed) {
            this.canExit = totalMinutes <= 10;
        } else if (log.getEntryType() == ParkingLog.EntryType.NORMAL) {
            this.canExit = true;
        } else {
            int applied = log.getAppliedMinutes() != null ? log.getAppliedMinutes() : 0;
            this.canExit = totalMinutes <= 10 || totalMinutes == 0 || applied >= totalMinutes;
        }

        // 출차 취소 가능 여부 (출차 후 10분 이내)
        this.canCancelExit = log.getExitTime() != null
                && Duration.between(log.getExitTime(), LocalDateTime.now()).toMinutes() <= 10;
    }

    public ParkingLogResponse(ParkingLog log) {
        this(log, 0);
    }

    private String formatMinutes(long minutes) {
        if (minutes >= 60) {
            return (minutes / 60) + "시간 " + (minutes % 60) + "분";
        }
        return minutes + "분";
    }
}