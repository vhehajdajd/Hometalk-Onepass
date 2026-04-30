package com.hometalk.onepass.dashboard.entity.notification;

import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.billing.dto.BillingDetailResponse;
import com.hometalk.onepass.billing.entity.BillingDetail;
import com.hometalk.onepass.billing.entity.BillingStatus;
import com.hometalk.onepass.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)      // JPA 스펙 상 기본 생성자 필수, PROTECTED로 외부 직접 생성 차단함.
@AllArgsConstructor(access = AccessLevel.PRIVATE)       // @Builder 내부 동작용 전체 생성자, PRIVATE으로 외부 노출 차단함.
@Builder  // id를 제외하고 필요한 필드만 선택적으로 주입 가능함. 예: Book.builder().title("AI의 미래").price(30000).build()
@Entity
@Table(name = "AlarmNotificationToBilling")  // 테이블명 명시. 생략 시 클래스명 기반 자동 지정됨. 테이블명은 복수형 사용.
public class NotificationToBilling extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "module_name", nullable = false, length = 50)
    private String moduleName;         // 알림 발생 모듈

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;          // 등록한 회원 ID FK

    @Column(name = "category_alarm", nullable = false, length = 50)
    private String categoryAlarm;      // 모듈별 세부 분류

    @Column(nullable = false, length = 500)
    private String message;             // 알림 내용 메시지

//    @Column(name = "reference_id")
//    private Long referenceId;

    @Column(name = "is_read")
    private Boolean isRead;            // 읽음 여부 상태

    @Column(nullable = true)
    private LocalDateTime deletedAt;            // 삭제 시각

    // 관리비
    @Column(name = "billing_month", nullable = false, unique = true, length = 50)
    private String billingMonth;       // 청구월 (예: 2026-03)

    @Column(name = "total_amount",nullable = false, precision = 12, scale = 0)
    private BigDecimal totalAmount;     // 합계 금액

    @Builder.Default
    @Enumerated(EnumType.STRING)    // DB에 문자열(UNPAID)로 저장되도록 설정
    private BillingStatus status = BillingStatus.UNPAID;    // UNPAID / PAID (관리비 납부 유무)

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;      // 납기일

    @JdbcTypeCode(SqlTypes.JSON)
    private List<BillingDetailResponse.ItemDetail> billingItems =  new ArrayList<>();      // 전기료, 수도료, 청소비 등 항목명, 개별 항목 금액

    public void initBillingItems(List<BillingDetail> details) {


        this.billingItems.clear();

        BillingDetailResponse.ItemDetail itemDetail;
        for (BillingDetail detail : details) {

            itemDetail = BillingDetailResponse.ItemDetail.builder()
                    .itemName(detail.getItemName())
                    .itemAmount(detail.getItemAmount())
                    .build();


            this.billingItems.add(itemDetail);
        }
    }

    //    // 관리비
//    private String billing_month;       // 청구월 (예: 2026-03)
//    private int total_amount;           // 합계 금액
//    private boolean status;             // UNPAID / PAID (관리비 납부 유무)
//    private LocalDateTime due_date;     // 납기일
//    private List<String> item_name;           // 전기료, 수도료, 청소비 등 항목명
//    private List<Integer> item_amount;            // 개별 항목 금액
}
