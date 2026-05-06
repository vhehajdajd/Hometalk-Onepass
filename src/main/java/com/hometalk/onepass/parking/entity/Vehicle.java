package com.hometalk.onepass.parking.entity;

import com.hometalk.onepass.common.entity.BaseSoftDeleteEntity;
import com.hometalk.onepass.auth.entity.Household;
import com.hometalk.onepass.auth.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "vehicles")
@Getter
@NoArgsConstructor
public class Vehicle extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vehicle_id")
    private Long vehicleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id")
    private Household household;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "vehicle_number", nullable = false, unique = true, length = 20)
    private String vehicleNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status;

    @Column(length = 50)
    private String model;

    @Column(name = "vehicle_type", length = 20)
    private String vehicleType;

    public Vehicle(Household household, User user, String vehicleNumber, String model, String vehicleType) {
        if (vehicleNumber == null || vehicleNumber.isBlank()) {
            throw new IllegalArgumentException("차량 번호는 필수입니다.");
        }
        this.household = household;
        this.user = user;
        this.vehicleNumber = vehicleNumber;
        this.model = model;
        this.vehicleType = vehicleType;
        this.status = VehicleStatus.PENDING;
    }

    public void approve() {
        this.status = VehicleStatus.APPROVED;
    }

    public void reject() {
        this.status = VehicleStatus.REJECTED;
    }

    public void pending() {
        this.status = VehicleStatus.PENDING;
    }

    public enum VehicleStatus {
        PENDING, APPROVED, REJECTED
    }
}