package com.hometalk.onepass.notification.controller;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.notification.entity.NotificationTargetRole;
import com.hometalk.onepass.notification.service.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationSseController {

    private final SseEmitterManager sseEmitterManager;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        // ✅ null 체크
        if (user == null) return new SseEmitter(0L);

        // ✅ role 함께 전달
        NotificationTargetRole role = switch (user.getRole()) {
            case ADMIN -> NotificationTargetRole.ADMIN;
            default    -> NotificationTargetRole.RESIDENT;
        };
        return sseEmitterManager.connect(user.getUserId(), role, lastEventId);
    }
}