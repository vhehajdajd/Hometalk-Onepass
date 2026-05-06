package com.hometalk.onepass.parking.entity;

import com.hometalk.onepass.common.entity.BaseTimeEntity;
import com.hometalk.onepass.auth.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_approvals")
@Getter
@NoArgsConstructor
public class VehicleApproval extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "approval_id")
    private Long approvalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private User admin;

    @Column(name = "document_path", nullable = false, columnDefinition = "TEXT")
    private String documentPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus status;

    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public VehicleApproval(Vehicle vehicle, String documentPath) {
        if (vehicle == null) {
            throw new IllegalArgumentException("차량 정보는 필수입니다.");
        }
        if (documentPath == null || documentPath.isBlank()) {
            throw new IllegalArgumentException("증빙 서류는 필수입니다.");
        }
        this.vehicle = vehicle;
        this.documentPath = documentPath;
        this.status = ApprovalStatus.PENDING;
    }

    public void approve(User admin) {
        if (this.status != ApprovalStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 요청입니다.");
        }
        this.status = ApprovalStatus.APPROVED;
        this.admin = admin;
        this.processedAt = LocalDateTime.now();
    }

    public void reject(User admin, String reason) {
        if (this.status != ApprovalStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 요청입니다.");
        }
        this.status = ApprovalStatus.REJECTED;
        this.admin = admin;
        this.rejectReason = reason;
        this.processedAt = LocalDateTime.now();
    }

    public enum ApprovalStatus {
        PENDING, APPROVED, REJECTED
    }
}