package com.hometalk.onepass.parking.dto.response;

import com.hometalk.onepass.parking.entity.VehicleApproval;
import lombok.Getter;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class VehicleApprovalResponse {

    private Long approvalId;
    private Long vehicleId;
    private String vehicleNumber;
    private String model;
    private String vehicleType;
    private String userName;
    private String household;
    private List<String> documentPaths; // 증빙 서류 경로 목록
    private String status;
    private String rejectReason;
    private LocalDateTime processedAt;
    private String createdAt;
    private int approvedCount; // 해당 세대 승인된 차량 수

    public VehicleApprovalResponse(VehicleApproval approval) {
        this.approvalId = approval.getApprovalId();
        this.vehicleId = approval.getVehicle().getVehicleId();
        this.vehicleNumber = approval.getVehicle().getVehicleNumber();
        this.model = approval.getVehicle().getModel();
        this.vehicleType = approval.getVehicle().getVehicleType();
        this.userName = approval.getVehicle().getUser().getName();
        this.household = approval.getVehicle().getHousehold().getDong() + " "
                + approval.getVehicle().getHousehold().getHo();

        // 증빙 서류 경로 목록 (콤마로 구분된 경로를 파일명만 추출하여 리스트로 변환)
        this.documentPaths = approval.getDocumentPath() != null
                ? Arrays.stream(approval.getDocumentPath().split(","))
                .map(path -> "/hometop/uploads/" + Paths.get(path.trim()).getFileName().toString())
                .collect(Collectors.toList())
                : List.of();

        this.status = approval.getStatus().name();
        this.rejectReason = approval.getRejectReason();
        this.processedAt = approval.getProcessedAt();
        this.createdAt = approval.getCreatedAt() != null
                ? approval.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"))
                : "-";
    }

    public void setApprovedCount(int approvedCount) {
        this.approvedCount = approvedCount;
    }
}