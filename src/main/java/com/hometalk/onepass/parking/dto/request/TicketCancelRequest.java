package com.hometalk.onepass.parking.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TicketCancelRequest {

    private Long parkingId;
    private String ticketType;
    private int count; // 티켓 수량별로 취소하기 위해 추가
}