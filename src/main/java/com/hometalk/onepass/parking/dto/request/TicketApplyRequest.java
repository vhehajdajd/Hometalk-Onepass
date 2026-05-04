package com.hometalk.onepass.parking.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TicketApplyRequest {

    private Long parkingId;
    private String ticketType;
    private int count;
}