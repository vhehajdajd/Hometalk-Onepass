package com.hometalk.onepass.billing.entity;

import com.hometalk.onepass.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "billing_detail",
        indexes = {
                @Index(name = "idx_billing_detail_billing_id", columnList = "billing_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class BillingDetail extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "billing_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_billing_detail_billing_id")
    )
    private Billing billing;

    @Column(nullable = false, length = 100)
    private String itemName;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal itemAmount;

    @Column(nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}