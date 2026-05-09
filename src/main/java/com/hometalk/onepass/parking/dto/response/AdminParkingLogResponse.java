package com.hometalk.onepass.parking.dto.response;

import com.hometalk.onepass.parking.entity.ParkingLog;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
public class AdminParkingLogResponse {

    private final Long parkingId;
    private final String vehicleNumber;
    private final String household;
    private final String vehicleType;
    private final String entryTime;
    private final String exitTime;
    private final String parkingTime;
    private final String settlement;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public AdminParkingLogResponse(ParkingLog log) {
        this.parkingId     = log.getParkingId();
        this.vehicleNumber = log.getVehicleNumber();
        this.household = log.getHousehold() != null
                ? log.getHousehold().getDong() + " " + log.getHousehold().getHo()
                : "-";
        this.vehicleType = switch (log.getEntryType()) {
            case RESERVATION -> "방문 예약";
            case NORMAL      -> "입주자";
            case MANUAL      -> "수동 입차";
        };
        this.entryTime   = log.getEntryTime() != null
                ? log.getEntryTime().format(FMT) : "-";
        this.exitTime    = log.getExitTime() != null
                ? log.getExitTime().format(FMT) : "주차 중";
        this.parkingTime = log.getTotalMinutes() != null
                ? formatMinutes(log.getTotalMinutes()) : "-";

        // 정산 내역
        if (log.getExitTime() == null) {
            this.settlement = "주차 중";
        } else if (log.getTotalMinutes() == null) {
            this.settlement = "-";
        } else if (log.getEntryType() == ParkingLog.EntryType.NORMAL) {
            // 입주자는 항상 무료
            this.settlement = "무료";
        } else {
            int total   = log.getTotalMinutes();
            int applied = log.getAppliedMinutes() != null ? log.getAppliedMinutes() : 0;
            int charged = Math.max(0, total - applied);

            if (charged > 0) {
                this.settlement = "초과 " + formatMinutes(charged);
            } else if (applied > 0) {
                this.settlement = "티켓 사용";
            } else {
                this.settlement = "무료";
            }
        }
    }

    private String formatMinutes(long minutes) {
        if (minutes >= 60) {
            return (minutes / 60) + "시간 " + (minutes % 60) + "분";
        }
        return minutes + "분";
    }
}