package com.hometalk.onepass.dashboard.entity.dashboard;

import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.parking.entity.ParkingTicket;
import jakarta.persistence.*;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)      // JPA 스펙 상 기본 생성자 필수, PROTECTED로 외부 직접 생성 차단함.
@AllArgsConstructor(access = AccessLevel.PRIVATE)       // @Builder 내부 동작용 전체 생성자, PRIVATE으로 외부 노출 차단함.
@Builder  // id를 제외하고 필요한 필드만 선택적으로 주입 가능함. 예: Book.builder().title("AI의 미래").price(30000).build()
@Entity
@Table(name = "ParkingToDashBoardInfo")
public class DashBoardParking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParkingTicket.TicketType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_count", nullable = false)
    private int totalCount;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    //    TYPE	티켓 종류	{ "type" }
    //    USER_ID	등록한 회원 ID	{ "user_id" }
    //    TOTAL_COUNT	총 보유 수량	{ "total_count" }
    //    USED_COUNT	사용 완료 수량	{ "used_count" }
}
