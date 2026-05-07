package com.hometalk.onepass.notification.controller;

import com.hometalk.onepass.notification.dto.NotificationResponse;
import com.hometalk.onepass.notification.entity.NotificationTargetRole;
import com.hometalk.onepass.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 알림 API 컨트롤러 (REST)
 *
 * 엔드포인트:
 *   GET  /api/notification/list          - 알림 목록 (페이징)
 *   GET  /api/notification/unread-count  - 미읽음 수
 *   POST /api/notification/{id}/read     - 단건 읽음 처리
 *   POST /api/notification/read-all      - 전체 읽음 처리
 */
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationApiController {

    private final NotificationService notificationService;

    // ─────────────────── 1. 알림 목록 조회 (페이징) ───────────────────
    @GetMapping("/list")
    public ResponseEntity<Page<NotificationResponse>> getList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
            // TODO: Security 연동 후 @AuthenticationPrincipal CustomUserDetails user 추가
    ) {
        // TODO: Security 연동 후 user.getId(), user.getRole() 사용
        Long userId = getCurrentUserId();
        NotificationTargetRole role = getCurrentUserRole();

        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponse> result =
                notificationService.getNotifications(userId, role, pageable);

        return ResponseEntity.ok(result);
    }

    // ─────────────────── 2. 미읽음 수 조회 ───────────────────
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        // TODO: Security 연동 후 @AuthenticationPrincipal로 변경
        Long userId = getCurrentUserId();
        NotificationTargetRole role = getCurrentUserRole();

        long count = notificationService.countUnread(userId, role);
        return ResponseEntity.ok(Map.of("count", count));
    }

    // ─────────────────── 3. 단건 읽음 처리 ───────────────────
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        // TODO: Security 연동 후 @AuthenticationPrincipal로 변경
        Long userId = getCurrentUserId();

        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok().build();
    }

    // ─────────────────── 4. 전체 읽음 처리 ───────────────────
    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        // TODO: Security 연동 후 @AuthenticationPrincipal로 변경
        Long userId = getCurrentUserId();
        NotificationTargetRole role = getCurrentUserRole();

        notificationService.markAllAsRead(userId, role);
        return ResponseEntity.ok().build();
    }

    // ─────────────────── 임시 사용자 정보 (Security 연동 전) ───────────────────
    // TODO: Security 연동 완료되면 이 두 메서드 삭제
    private Long getCurrentUserId() {
        return 2L; // 임시 하드코딩
    }

    private NotificationTargetRole getCurrentUserRole() {
        return NotificationTargetRole.RESIDENT; // 임시 하드코딩
    }
}