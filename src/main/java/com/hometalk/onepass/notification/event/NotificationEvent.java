package com.hometalk.onepass.notification.event;

import com.hometalk.onepass.notification.entity.NotificationTargetRole;
import com.hometalk.onepass.notification.entity.NotificationType;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class NotificationEvent {

    private final Long                   userId;       // null = 전체 발송
    private final NotificationTargetRole targetRole;
    private final NotificationType       type;
    private final String                 title;
    private final String                 message;
    private final String                 link;         // null 허용
    private final Long                   referenceId;  // null 허용 (도메인 ID)
}