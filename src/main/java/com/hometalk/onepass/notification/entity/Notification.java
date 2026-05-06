package com.hometalk.onepass.notification.entity;

import com.hometalk.onepass.common.entity.BaseTimeEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import com.hometalk.onepass.billing.dto.BillingMonthlyStats;
import com.hometalk.onepass.billing.dto.UserOldestUnpaidProjection;

@Entity
@Table(name = "notification", indexes = {
        @Index(name = "idx_noti_user_role", columnList = "user_id, target_role"),
        @Index(name = "idx_noti_expired",   columnList = "expired_at"),
        @Index(name = "idx_noti_updated",   columnList = "updated_at"),
        @Index(name = "idx_noti_reference", columnList = "reference_id")
})
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;        // null = 전체 발송
    private Long referenceId;   // ← snake_case 아님! camelCase로 작성 (DB 컬럼은 자동으로 reference_id)

    @Enumerated(EnumType.STRING)
    private NotificationTargetRole targetRole;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private String title;
    private String message;
    private String link;        // null 허용

    private LocalDateTime expiredAt;



}