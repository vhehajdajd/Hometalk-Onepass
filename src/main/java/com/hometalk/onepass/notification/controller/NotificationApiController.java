package com.hometalk.onepass.notification.controller;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.notification.dto.NotificationResponse;
import com.hometalk.onepass.notification.entity.NotificationTargetRole;
import com.hometalk.onepass.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationApiController {

    private final NotificationService notificationService;

    // ─────────────────── 1. 알림 목록 조회 (페이징) ───────────────────
    @GetMapping("/list")
    public ResponseEntity<Page<NotificationResponse>> getList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        // ✅ null 체크
        if (user == null) return ResponseEntity.ok(Page.empty());

        Long userId = user.getUserId();
        NotificationTargetRole role = resolveRole(user);

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(notificationService.getNotifications(userId, role, pageable));
    }

    // ─────────────────── 2. 미읽음 수 조회 ───────────────────
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        // ✅ null 체크
        if (user == null) return ResponseEntity.ok(Map.of("count", 0L));

        Long userId = user.getUserId();
        NotificationTargetRole role = resolveRole(user);

        long count = notificationService.countUnread(userId, role);
        return ResponseEntity.ok(Map.of("count", count));
    }

    // ─────────────────── 3. 단건 읽음 처리 ───────────────────
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        // ✅ null 체크
        if (user == null) return ResponseEntity.ok().build();

        notificationService.markAsRead(id, user.getUserId());
        return ResponseEntity.ok().build();
    }

    // ─────────────────── 4. 전체 읽음 처리 ───────────────────
    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        // ✅ null 체크
        if (user == null) return ResponseEntity.ok().build();

        Long userId = user.getUserId();
        NotificationTargetRole role = resolveRole(user);

        notificationService.markAllAsRead(userId, role);
        return ResponseEntity.ok().build();
    }

    // ─────────────────── 5. 전체 삭제 처리 ───────────────────
    @DeleteMapping("/delete-all")
    public ResponseEntity<Void> deleteAll(
            @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) return ResponseEntity.ok().build();
        notificationService.deleteAll(user.getUserId(), resolveRole(user));
        return ResponseEntity.ok().build();
    }

    // ─────────────────── role 변환 유틸 ───────────────────
    private NotificationTargetRole resolveRole(CustomUserDetails user) {
        return switch (user.getRole()) {
            case ADMIN -> NotificationTargetRole.ADMIN;
            default    -> NotificationTargetRole.RESIDENT;
        };
    }
}