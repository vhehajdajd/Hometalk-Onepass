package com.hometalk.onepass.reservation.entity;

import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.facility.entity.Facility;
import jakarta.persistence.*;
import jakarta.persistence.Id;
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

    // 어디 예약 했는지 (시설 정보와 연결)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id")
    private Facility facility;

    // 누가 예약 ? (회원 정보와 연결)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // DB의 user_id 컬럼과 매핑
    private User user;      // 병합 후 import 예정

    // 언제부터 언제까지 ?
    @Embedded
    private ReservationTime reservationTime;

    // 예약 상태
    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    // 예약 취소 로직
    public void cancel() {
        if (this.status == ReservationStatus.COMPLETED) { // 이미 완료된 건 취소 불가 정책 등
            throw new RuntimeException("이미 완료된 예약은 취소할 수 없습니다.");
        }
        this.status = ReservationStatus.CANCELED;
    }
}
