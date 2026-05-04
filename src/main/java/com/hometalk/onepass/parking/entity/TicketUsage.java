package com.hometalk.onepass.parking.entity;

import com.hometalk.onepass.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ticket_usage")
@Getter
@NoArgsConstructor
public class TicketUsage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usage_id")
    private Long usageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_id", nullable = false)
    private ParkingLog parkingLog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private ParkingTicket ticket;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    @Column(name = "used_minutes", nullable = false)
    private int usedMinutes;

    public TicketUsage(ParkingLog parkingLog, ParkingTicket ticket, int usedCount) {
        if (parkingLog == null) {
            throw new IllegalArgumentException("주차 기록은 필수입니다.");
        }
        if (ticket == null) {
            throw new IllegalArgumentException("티켓 정보는 필수입니다.");
        }
        if (usedCount <= 0) {
            throw new IllegalArgumentException("사용 수량은 1 이상이어야 합니다.");
        }
        this.parkingLog = parkingLog;
        this.ticket = ticket;
        this.usedCount = usedCount;
        this.usedMinutes = ticket.getType().toMinutes(usedCount);
        ticket.use(usedCount);
    }

    // 생성자 밖으로 분리
    public void updateCount(int newCount) {
        if (newCount <= 0) throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
        this.usedCount = newCount;
        this.usedMinutes = ticket.getType().toMinutes(newCount);
    }
}