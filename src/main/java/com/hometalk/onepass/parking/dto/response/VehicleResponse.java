package com.hometalk.onepass.parking.dto.response;

import com.hometalk.onepass.parking.entity.ParkingLog;
import com.hometalk.onepass.parking.entity.Vehicle;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
public class VehicleResponse {

    private Long vehicleId;
    private String vehicleNumber;
    private String model;
    private String vehicleType;
    private String status;
    private String userName;
    private String household;
    private boolean isParked;
    private String entryTime;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public VehicleResponse(Vehicle vehicle, ParkingLog parkingLog) {
        this.vehicleId = vehicle.getVehicleId();
        this.vehicleNumber = vehicle.getVehicleNumber();
        this.model = vehicle.getModel();
        this.vehicleType = vehicle.getVehicleType();
        this.status = vehicle.getStatus().name();
        this.userName = vehicle.getUser() != null ? vehicle.getUser().getName() : "";
        this.household = vehicle.getHousehold() != null
                ? vehicle.getHousehold().getDong() + " " + vehicle.getHousehold().getHo()
                : "";
        this.isParked = parkingLog != null;
        this.entryTime = parkingLog != null
                ? parkingLog.getEntryTime().format(FMT)
                : null;
    }

    // 기존 호환용
    public VehicleResponse(Vehicle vehicle) {
        this(vehicle, null);
    }
}