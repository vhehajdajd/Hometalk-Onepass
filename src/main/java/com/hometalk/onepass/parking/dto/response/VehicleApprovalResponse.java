package com.hometalk.onepass.parking.dto.response;

import com.hometalk.onepass.parking.entity.VehicleApproval;
import lombok.Getter;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
public class VehicleApprovalResponse {

    private Long approvalId;
    private Long vehicleId;
    private String vehicleNumber;
    private String model;
    private String vehicleType;
    private String userName;
    private String household;
    private String documentPath;
    private String status;
    private String rejectReason;
    private LocalDateTime processedAt;
    private String createdAt;

    public VehicleApprovalResponse(VehicleApproval approval) {
        this.approvalId = approval.getApprovalId();
        this.vehicleId = approval.getVehicle().getVehicleId();
        this.vehicleNumber = approval.getVehicle().getVehicleNumber();
        this.model = approval.getVehicle().getModel();
        this.vehicleType = approval.getVehicle().getVehicleType();
        this.userName = approval.getVehicle().getUser().getName();
        this.household = approval.getVehicle().getHousehold().getDong() + " "
                + approval.getVehicle().getHousehold().getHo();
        this.documentPath = approval.getDocumentPath() != null
                ? "/hometop/uploads/" + Paths.get(approval.getDocumentPath()).getFileName().toString()
                : null;
        this.status = approval.getStatus().name();
        this.rejectReason = approval.getRejectReason();
        this.processedAt = approval.getProcessedAt();
        this.createdAt = approval.getCreatedAt() != null
                ? approval.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"))
                : "-";
    }
}