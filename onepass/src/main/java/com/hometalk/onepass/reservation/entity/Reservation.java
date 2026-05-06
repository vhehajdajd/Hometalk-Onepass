package com.hometalk.onepass.reservation.entity;

import com.hometalk.onepass.facility.entity.Facility;
import jakarta.persistence.*;
import jakarta.persistence.Id;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder

public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어디 예약 했는지 (시설 정보와 연결)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id")
    private Facility facility;

    // 누가 예약 ? (회원 정보와 연결)
    private String memberId;        // 이후 로그인 기능 구현 후 Member로 바꿀 예정

    // 언제부터 언제까지 ?
    @Embedded
    private ReservationTime reservationTime;

    // 예약 상태
    @Enumerated(EnumType.STRING)
    private ReservationStatus status;


}
