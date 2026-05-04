package com.hometalk.onepass.parking.entity;

import com.hometalk.onepass.common.entity.BaseSoftDeleteEntity;
import com.hometalk.onepass.auth.entity.Household;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "visit_reservations")
@Getter
@NoArgsConstructor
public class VisitReservation extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long reservationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = true) // 수동 입차 시 null 허용
    private Household household;

    @Column(name = "vehicle_number", nullable = false, length = 20)
    private String vehicleNumber;

    @Column(nullable = false, length = 100)
    private String purpose;

    // 사전 예약 시 → 방문 예정 시각
    // 수동 입차 시 → 입차 시각 (확장 시 enteredAt 컬럼 분리 고려)
    @Column(name = "reserved_at", nullable = false)
    private LocalDateTime reservedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    // ─── 사전 예약 생성 ──────────────────────────────────────────
    public VisitReservation(Household household, String vehicleNumber,
                            String purpose, LocalDateTime reservedAt) {
        if (household == null) {
            throw new IllegalArgumentException("세대 정보는 필수입니다.");
        }
        if (vehicleNumber == null || vehicleNumber.isBlank()) {
            throw new IllegalArgumentException("차량 번호는 필수입니다.");
        }
        if (purpose == null || purpose.isBlank()) {
            throw new IllegalArgumentException("방문 목적은 필수입니다.");
        }
        if (reservedAt == null) {
            throw new IllegalArgumentException("방문 예정 시간은 필수입니다.");
        }
        if (reservedAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("방문 예정 시간은 현재 이후여야 합니다.");
        }
        this.household = household;
        this.vehicleNumber = vehicleNumber;
        this.purpose = purpose;
        this.reservedAt = reservedAt;
        this.status = ReservationStatus.RESERVED;
    }

    // ─── 수동 입차 생성 (동호수 모를 시 household null 허용) ─────
    public static VisitReservation ofManual(Household household,
                                            String vehicleNumber,
                                            String purpose) {
        if (vehicleNumber == null || vehicleNumber.isBlank()) {
            throw new IllegalArgumentException("차량 번호는 필수입니다.");
        }
        if (purpose == null || purpose.isBlank()) {
            throw new IllegalArgumentException("방문 목적은 필수입니다.");
        }
        VisitReservation r = new VisitReservation();
        r.household = household; // null 가능
        r.vehicleNumber = vehicleNumber;
        r.purpose = purpose;
        r.reservedAt = LocalDateTime.now();
        r.status = ReservationStatus.PENDING_CONFIRM;
        return r;
    }

    // ─── 상태 변경 ───────────────────────────────────────────────
    public void update(String vehicleNumber, String purpose, LocalDateTime reservedAt) {
        if (this.status != ReservationStatus.RESERVED) {
            throw new IllegalStateException("예약 상태에서만 수정할 수 있습니다.");
        }
        if (vehicleNumber != null && !vehicleNumber.isBlank()) {
            this.vehicleNumber = vehicleNumber;
        }
        if (purpose != null && !purpose.isBlank()) {
            this.purpose = purpose;
        }
        if (reservedAt != null) {
            if (reservedAt.isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("방문 예정 시간은 현재 이후여야 합니다.");
            }
            this.reservedAt = reservedAt;
        }
    }

    public void cancel() {
        if (this.status != ReservationStatus.RESERVED) {
            throw new IllegalStateException("예약 상태에서만 취소할 수 있습니다.");
        }
        this.status = ReservationStatus.CANCELLED;
    }

    public void enter() {
        if (this.status == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("취소된 예약은 입차 처리할 수 없습니다.");
        }
        if (this.status != ReservationStatus.RESERVED &&
                this.status != ReservationStatus.PENDING_CONFIRM) {
            throw new IllegalStateException("입차 처리할 수 없는 상태입니다.");
        }
        this.status = ReservationStatus.ENTERED;
    }

    // ─── 세대 매칭 (수동 입차 후 입주자가 등록) ──────────────────
    public void matchHousehold(Household household) {
        if (this.status != ReservationStatus.PENDING_CONFIRM) {
            throw new IllegalStateException("대기 상태에서만 세대 매칭할 수 있습니다.");
        }
        this.household = household;
        this.status = ReservationStatus.ENTERED;
    }

    public enum ReservationStatus {
        RESERVED,        // 사전 예약 완료
        ENTERED,         // 입차 완료
        CANCELLED,       // 예약 취소
        PENDING_CONFIRM  // 수동 입차 후 세대 확인 대기
    }
}