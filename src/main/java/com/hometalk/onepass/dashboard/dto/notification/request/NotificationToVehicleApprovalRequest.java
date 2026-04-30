package com.hometalk.onepass.dashboard.dto.notification.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NotificationToVehicleApprovalRequest {

    private Long approvalId;
    private String rejectReason;
}
