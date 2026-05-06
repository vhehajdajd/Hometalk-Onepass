package com.hometalk.onepass.parking.dto.response;

import com.hometalk.onepass.parking.entity.VisitReservation;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
public class UnregisteredVehicleResponse {

    private final Long reservationId;
    private final String vehicleNumber;
    private final String purpose;
    private final String enteredAt;

    public UnregisteredVehicleResponse(VisitReservation reservation) {
        this.reservationId = reservation.getReservationId();
        this.vehicleNumber = reservation.getVehicleNumber();
        this.purpose = reservation.getPurpose();
        this.enteredAt = reservation.getReservedAt()
                .format(DateTimeFormatter.ofPattern("MM.dd HH:mm"));
    }
}