package com.hometalk.onepass.dashboard.dto.notification.response;


import com.hometalk.onepass.dashboard.entity.notification.NotificationToParking;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationToParkingDto {

    private Long id;
    private String moduleName;         // 알림 발생 모듈
    private String categoryAlarm;      // 모듈별 세부 분류
    //private Boolean isRead;            // 읽음 여부 상태
    private String vehicleNumber;      // 차량 번호 전체
    private String message;            // 메세지 내용
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;            // 삭제 시각

    /*  Entity --> DTO 변환 메서드 (정적 팩토리 메서드) */
    public static NotificationToParkingDto from(NotificationToParking notification) {
        return NotificationToParkingDto.builder()
                .id(notification.getId())
                .moduleName(notification.getModuleName())
                .categoryAlarm(notification.getCategoryAlarm())
                //.isRead(notification.getIsRead())
                .vehicleNumber(notification.getVehicleNumber())
                .message(notification.getMessage())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .deletedAt(notification.getDeletedAt())
                .build();
    }
}
