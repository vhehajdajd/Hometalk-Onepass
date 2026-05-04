package com.hometalk.onepass.parking.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VehicleApprovalRequest {

    private Long approvalId;
    private String rejectReason;
}