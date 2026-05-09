package com.hometalk.onepass.parking.dto.request;

import com.hometalk.onepass.parking.entity.ParkingTicket;
import java.util.List;

public record BulkTicketIssueRequest(
        List<Long> householdIds,
        ParkingTicket.TicketType type,
        int totalCount
) {}