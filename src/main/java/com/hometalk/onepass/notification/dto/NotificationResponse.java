package com.hometalk.onepass.notification.dto;

import com.hometalk.onepass.notification.entity.NotificationType;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class NotificationResponse {

    private final Long    id;
    private final String  type;
    private final String  category;
    private final String  icon;
    private final String  title;
    private final String  message;
    private final String  link;       // null 허용
    private final Boolean isRead;
    private final String  createdAt;

    // ✅ JPQL new 생성자 — Repository 쿼리에서 직접 사용
    public NotificationResponse(
            Long id,
            NotificationType type,
            String title,
            String message,
            String link,
            Boolean isRead,
            LocalDateTime createdAt
    ) {
        this.id        = id;
        this.type      = type.name();
        this.category  = type.getCategory();
        this.icon      = type.getIcon();
        this.title     = title;
        this.message   = message;
        this.link      = link;
        this.isRead    = isRead != null && isRead;
        this.createdAt = createdAt.toString();
    }
}