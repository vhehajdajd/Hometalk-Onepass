package com.hometalk.onepass.parking.dto.response;

import lombok.Getter;

@Getter
public class ExitResponse {

    private Long parkingId;
    private String vehicleNumber;
    private String household;
    private int totalMinutes;
    private int appliedMinutes;
    private int remainingMinutes;
    private boolean canExit;
    private String parkingTime;
    private String ticketInfo;

    public ExitResponse(Long parkingId, String vehicleNumber, String household,
                        int totalMinutes, int appliedMinutes) {
        this.parkingId = parkingId;
        this.vehicleNumber = vehicleNumber;
        this.household = household;
        this.totalMinutes = totalMinutes;
        this.appliedMinutes = appliedMinutes;
        this.remainingMinutes = Math.max(0, totalMinutes - appliedMinutes);
        this.canExit = totalMinutes <= appliedMinutes;
        this.parkingTime = formatMinutes(totalMinutes);
        this.ticketInfo = formatMinutes(appliedMinutes);
    }

    private String formatMinutes(int minutes) {
        int hours = minutes / 60;
        int mins = minutes % 60;
        return hours + "시간 " + mins + "분";
    }
}