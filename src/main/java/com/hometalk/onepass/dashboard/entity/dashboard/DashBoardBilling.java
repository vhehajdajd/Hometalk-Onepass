package com.hometalk.onepass.dashboard.entity.dashboard;

import com.hometalk.onepass.billing.entity.BillingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Setter
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)      // JPA 스펙 상 기본 생성자 필수, PROTECTED로 외부 직접 생성 차단함.
@AllArgsConstructor(access = AccessLevel.PRIVATE)       // @Builder 내부 동작용 전체 생성자, PRIVATE으로 외부 노출 차단함.
@Builder  // id를 제외하고 필요한 필드만 선택적으로 주입 가능함. 예: Book.builder().title("AI의 미래").price(30000).build()
@Entity
@Table(name = "BillingToDashBoardInfo")
public class DashBoardBilling {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private BillingStatus status = BillingStatus.UNPAID;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 100)
    private String itemName;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal itemAmount;

    @Column(nullable = false, length = 7)
    private String billingMonth;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private LocalDate dueDate;
}
