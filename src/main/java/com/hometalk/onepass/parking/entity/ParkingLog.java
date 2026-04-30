package com.hometalk.onepass.parking.entity;

import com.hometalk.onepass.common.entity.BaseSoftDeleteEntity;
import com.hometalk.onepass.auth.entity.Household;
import com.hometalk.onepass.auth.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "parking_logs")
@Getter
@NoArgsConstructor
public class ParkingLog extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "parking_id")
    private Long parkingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @Column(name = "vehicle_number", nullable = false, length = 20)
    private String vehicleNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id")
    private Household household;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private VisitReservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private User staff;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false)
    private EntryType entryType;

    @Column(name = "entry_time", nullable = false)
    private LocalDateTime entryTime;

    @Column(name = "exit_time")
    private LocalDateTime exitTime;

    @Column(name = "total_minutes")
    private Integer totalMinutes;

    @Column(name = "applied_minutes")
    private Integer appliedMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParkingStatus status;

    // ─── 입차 처리 ───────────────────────────────────────────────
    public ParkingLog(Vehicle vehicle, String vehicleNumber, Household household,
                      VisitReservation reservation, User staff, EntryType entryType) {

        if (vehicleNumber == null || vehicleNumber.isBlank()) {
            throw new IllegalArgumentException("차량 번호는 필수입니다.");
        }
        if (entryType == null) {
            throw new IllegalArgumentException("입차 유형은 필수입니다.");
        }

        this.vehicle = vehicle;
        this.vehicleNumber = vehicleNumber;
        this.household = household;
        this.reservation = reservation;
        this.staff = staff;
        this.entryType = entryType;
        this.entryTime = LocalDateTime.now();
        this.status = ParkingStatus.PARKED;

        // MANUAL 타입은 세대 확인 대기 상태 유지를 위해 enter() 호출 안 함
        if (this.reservation != null && entryType != EntryType.MANUAL) {
            this.reservation.enter();
        }
    }

    // ─── 출차 처리 ───────────────────────────────────────────────
    public void exit(int totalMinutes, int appliedMinutes) {

        if (this.status != ParkingStatus.PARKED) {
            throw new IllegalStateException("이미 출차된 차량입니다.");
        }
        if (totalMinutes < 0) {
            throw new IllegalArgumentException("총 주차 시간은 0 이상이어야 합니다.");
        }
        if (appliedMinutes < 0) {
            throw new IllegalArgumentException("티켓 적용 시간은 0 이상이어야 합니다.");
        }
        if (appliedMinutes > totalMinutes) {
            throw new IllegalArgumentException("티켓 적용 시간은 총 주차 시간을 초과할 수 없습니다.");
        }

        this.exitTime = LocalDateTime.now();
        this.totalMinutes = totalMinutes;
        this.appliedMinutes = appliedMinutes;

        if (totalMinutes > appliedMinutes) {
            this.status = ParkingStatus.OVERSTAY;
        } else {
            this.status = ParkingStatus.EXITED;
        }
    }

    // ─── 세대 매칭 ───────────────────────────────────────────────
    public void matchHousehold(Household household) {
        if (household == null) {
            throw new IllegalArgumentException("세대 정보는 필수입니다.");
        }
        this.household = household;
    }

    public enum EntryType {
        NORMAL, RESERVATION, MANUAL
    }

    public enum ParkingStatus {
        PARKED, EXITED, OVERSTAY
    }

    // ─── 티켓 적용 시간 업데이트
    public void updateAppliedMinutes(int appliedMinutes) {
        if (appliedMinutes < 0) {
            throw new IllegalArgumentException("티켓 적용 시간은 0 이상이어야 합니다.");
        }
        this.appliedMinutes = appliedMinutes;
    }
}