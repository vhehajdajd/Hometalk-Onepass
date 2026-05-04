package com.hometalk.onepass.parking.dto.response;

import com.hometalk.onepass.parking.entity.ParkingTicket;
import lombok.Getter;

@Getter
public class TicketResponse {

    private Long ticketId;
    private String ticketType;
    private int totalCount;
    private int usedCount;
    private int remainingCount;
    private int issueYear;
    private int issueMonth;

    public TicketResponse(ParkingTicket ticket) {
        this.ticketId = ticket.getTicketId();
        this.ticketType = ticket.getType().name();
        this.totalCount = ticket.getTotalCount();
        this.usedCount = ticket.getUsedCount();
        this.remainingCount = ticket.getRemainingCount();
        this.issueYear = ticket.getIssueYear();
        this.issueMonth = ticket.getIssueMonth();
    }
}