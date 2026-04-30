package com.hometalk.onepass.parking.entity;
import com.hometalk.onepass.common.entity.BaseTimeEntity;
import com.hometalk.onepass.auth.entity.Household;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(
        name = "parking_tickets",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"household_id", "type", "issue_year", "issue_month"}
        )
)
// 정책: 매달 1일 자동 발급 (세대 + 타입 + 연/월 기준 1회)
@Getter
@NoArgsConstructor
public class ParkingTicket extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Long ticketId;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketType type;

    @Column(name = "total_count", nullable = false)
    private int totalCount;

    @Column(name = "used_count", nullable = false)
    private int usedCount = 0;

    // “언제 발급됐는지” 기록 (로그성)
    @Column(name = "issued_date", nullable = false)
    private LocalDate issuedDate;
    // 발급 기준 (월 단위)
    @Column(name = "issue_year", nullable = false)
    private int issueYear;
    @Column(name = "issue_month", nullable = false)
    private int issueMonth;
    public ParkingTicket(Household household, TicketType type, int totalCount, LocalDate issuedDate) {
        if (household == null) {
            throw new IllegalArgumentException("세대 정보는 필수입니다.");
        }
        if (type == null) {
            throw new IllegalArgumentException("티켓 종류는 필수입니다.");
        }
        if (totalCount <= 0 || totalCount > 1000) {
            throw new IllegalArgumentException("티켓 수량은 1~1000 사이여야 합니다.");
        }
        if (issuedDate == null) {
            throw new IllegalArgumentException("발급일은 필수입니다.");
        }
        if (issuedDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("발급일은 현재 이후일 수 없습니다.");
        }
        this.household = household;
        this.type = type;
        this.totalCount = totalCount;
        this.issuedDate = issuedDate;
        // 👉 발급 기준 자동 세팅
        this.issueYear = issuedDate.getYear();
        this.issueMonth = issuedDate.getMonthValue();
    }
    /**
     * 티켓 사용
     */
    public void use(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("사용 수량은 1 이상이어야 합니다.");
        }
        if (!isEnough(count)) {
            throw new IllegalStateException("티켓 잔여 수량이 부족합니다.");
        }
        this.usedCount += count;
    }
    /**
     * 잔여 수량
     */
    public int getRemainingCount() {
        return this.totalCount - this.usedCount;
    }
    /**
     * 사용 가능 여부
     */
    public boolean isEnough(int count) {
        return getRemainingCount() >= count;
    }
    /**
     * 티켓 타입 Enum (확장성 고려)
     */
    public enum TicketType {
        HOUR {
            @Override
            public int toMinutes(int count) {
                return count * 60;
            }
        },
        DAY {
            @Override
            public int toMinutes(int count) {
                return count * 60 * 24;
            }
        };
        public abstract int toMinutes(int count);
    }

    /**
     * 티켓 수량 복구 (취소 시)
     */
    public void restoreCount(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("복구 수량은 1 이상이어야 합니다.");
        }
        if (this.usedCount - count < 0) {
            throw new IllegalStateException("복구 수량이 사용 수량을 초과합니다.");
        }
        this.usedCount -= count;
    }

    /**
     * 수량 추가 (관리자 티켓 발급 시)
     */
    public void addCount(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("추가 수량은 1 이상이어야 합니다.");
        }
        this.totalCount += count;
    }
}