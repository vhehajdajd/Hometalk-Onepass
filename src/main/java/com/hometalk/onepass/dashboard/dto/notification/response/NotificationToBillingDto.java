package com.hometalk.onepass.dashboard.dto.notification.response;


import com.hometalk.onepass.billing.dto.BillingDetailResponse;
import com.hometalk.onepass.billing.entity.BillingStatus;
import com.hometalk.onepass.dashboard.entity.notification.NotificationToBilling;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.JoinColumn;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class NotificationToBillingDto {

    private Long id;
    private String moduleName;         // 알림 발생 모듈
    private String categoryAlarm;      // 모듈별 세부 분류
    //private Boolean isRead;            // 읽음 여부 상태
    private String message;            // 메세지 내용
    private String billingMonth;
    private BigDecimal totalAmount;     // 합계 금액
    private BillingStatus status;
    private LocalDate dueDate;      // 납기일
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;            // 삭제 시각

    private final List<BillingDetailResponse.ItemDetail> billingItems;

    /*  Entity --> DTO 변환 메서드 (정적 팩토리 메서드) */
    public static NotificationToBillingDto from(NotificationToBilling notification) {
        return NotificationToBillingDto.builder()
                .id(notification.getId())
                .moduleName(notification.getModuleName())
                .categoryAlarm(notification.getCategoryAlarm())
                .billingItems(notification.getBillingItems().stream()
                        .map(d -> BillingDetailResponse.ItemDetail.builder()
                                .itemName(d.getItemName())
                                .itemAmount(d.getItemAmount())
                                .build())
                        .collect(Collectors.toList()))
                .message(notification.getMessage())
                .billingMonth(notification.getBillingMonth())
                .totalAmount(notification.getTotalAmount())
                .status(notification.getStatus())
                .dueDate(notification.getDueDate())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .deletedAt(notification.getDeletedAt())
                .build();
    }
}
