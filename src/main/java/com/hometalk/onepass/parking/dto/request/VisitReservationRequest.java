package com.hometalk.onepass.parking.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class VisitReservationRequest {

    private String vehicleNumber;
    private String vehicleModel;
    private String purpose;
    private LocalDateTime reservedAt;
}