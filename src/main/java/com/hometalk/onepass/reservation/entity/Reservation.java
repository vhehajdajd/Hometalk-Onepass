package com.hometalk.onepass.reservation.entity;

import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.facility.entity.Facility;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "kjh_reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어디 예약 했는지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id")
    private Facility facility;

    // 누가 예약 했는지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // 예약 시간
    @Embedded
    private ReservationTime reservationTime;

    // 예약 상태
    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    // 관리자 취소 사유
    @Column(length = 255)
    private String cancelReason;

    // 예약 승인
    public void approve() {
        if (this.status == ReservationStatus.CANCELED) {
            throw new RuntimeException("취소된 예약은 승인할 수 없습니다.");
        }

        this.status = ReservationStatus.CONFIRMED;
    }

    // 일반 취소
    public void cancel() {
        if (this.status == ReservationStatus.COMPLETED) {
            throw new RuntimeException("이미 완료된 예약은 취소할 수 없습니다.");
        }

        this.status = ReservationStatus.CANCELED;
    }

    // 관리자 강제 취소
    public void adminCancel(String reason) {

        if (this.status == ReservationStatus.COMPLETED) {
            throw new RuntimeException("이미 완료된 예약은 취소할 수 없습니다.");
        }

        this.status = ReservationStatus.CANCELED;
        this.cancelReason = reason;
    }
}