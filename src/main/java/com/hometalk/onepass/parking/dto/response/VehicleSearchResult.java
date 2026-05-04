package com.hometalk.onepass.parking.dto.response;

import com.hometalk.onepass.parking.entity.Vehicle;
import com.hometalk.onepass.parking.entity.VisitReservation;
import lombok.Getter;

@Getter
public class VehicleSearchResult {

    private final String type;
    private final String vehicleNumber;
    private final String household;
    private final String purpose;
    private final Long vehicleId;
    private final Long reservationId;

    private VehicleSearchResult(String type, String vehicleNumber,
                                String household, String purpose,
                                Long vehicleId, Long reservationId) {
        this.type = type;
        this.vehicleNumber = vehicleNumber;
        this.household = household;
        this.purpose = purpose;
        this.vehicleId = vehicleId;
        this.reservationId = reservationId;
    }

    public static VehicleSearchResult ofResident(Vehicle vehicle) {
        String household = vehicle.getHousehold().getDong() + " "
                + vehicle.getHousehold().getHo();
        return new VehicleSearchResult(
                "RESIDENT",
                vehicle.getVehicleNumber(),
                household,
                null,
                vehicle.getVehicleId(),
                null
        );
    }

    public static VehicleSearchResult ofReservation(VisitReservation reservation) {
        String household = reservation.getHousehold() != null
                ? reservation.getHousehold().getDong() + " "
                + reservation.getHousehold().getHo()
                : "세대 미확인";
        return new VehicleSearchResult(
                "RESERVATION",
                reservation.getVehicleNumber(),
                household,
                reservation.getPurpose(),
                null,
                reservation.getReservationId()
        );
    }
}