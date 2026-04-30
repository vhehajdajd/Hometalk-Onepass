package com.hometalk.onepass.parking.dto.response;

import com.hometalk.onepass.parking.entity.ParkingLog;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
public class ParkingHistoryResponse {

    private final Long parkingId;
    private final String vehicleNumber;
    private final String entryTime;
    private final String exitTime;
    private final String entryType;
    private final String status;
    private final String settlement;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public ParkingHistoryResponse(ParkingLog log) {
        this.parkingId = log.getParkingId();
        this.vehicleNumber = log.getVehicleNumber();
        this.entryTime = log.getEntryTime() != null
                ? log.getEntryTime().format(FMT) : "-";
        this.exitTime = log.getExitTime() != null
                ? log.getExitTime().format(FMT) : "주차 중";
        this.entryType = switch (log.getEntryType()) {
            case NORMAL -> "입주자";
            case RESERVATION -> "방문 예약";
            case MANUAL -> "수동 입차";
        };
        this.status = log.getStatus().name();

        // 정산내역
        if (log.getExitTime() == null) {
            this.settlement = "주차 중";
        } else if (log.getTotalMinutes() == null) {
            this.settlement = "-";
        } else {
            int total = log.getTotalMinutes();
            int applied = log.getAppliedMinutes() != null ? log.getAppliedMinutes() : 0;
            int charged = Math.max(0, total - applied);
            if (charged == 0) {
                this.settlement = "무료";
            } else {
                this.settlement = "초과 " + formatMinutes(charged);
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