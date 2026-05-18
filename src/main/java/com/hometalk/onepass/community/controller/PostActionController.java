package com.hometalk.onepass.community.controller;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.community.exception.UnauthorizedAccessException;
import com.hometalk.onepass.community.service.PostActionService;
import com.hometalk.onepass.community.service.PostService;
import com.hometalk.onepass.community.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/*
    관리자용
 */

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostActionController {

    private final PostActionService postActionService;
    private final PostService postService;
    private final ReportService reportService;

    // 상단 고정
    @PostMapping("/{postId}/pin")
    public ResponseEntity<Void> togglePin(@PathVariable Long postId,
                                          Authentication authentication) {

        CustomUserDetails user = getLoginCustomUser(authentication);

        postActionService.togglePin(postId, user);
        return ResponseEntity.ok().build();
    }

    // 숨김 처리
    @PostMapping("/{postId}/hide")
    public ResponseEntity<Void> hidePost(@PathVariable Long postId,
                                         Authentication authentication) {

        CustomUserDetails user = getLoginCustomUser(authentication);

        postActionService.hidePost(postId, user);
        return ResponseEntity.ok().build();
    }

    // 숨김 해제
    @PostMapping("/{postId}/unhide")
    public ResponseEntity<Void> unhidePost(@PathVariable Long postId,
                                           Authentication authentication) {
        CustomUserDetails user = getLoginCustomUser(authentication);
        postActionService.unhidePost(postId, user);
        return ResponseEntity.ok().build();
    }

    // 신고 승인 및 처리 완료
    @PostMapping("/reports/{reportId}/resolve")
    public ResponseEntity<Void> resolveReport(@PathVariable Long reportId,
                                              Authentication authentication) {
        getLoginCustomUser(authentication);
        // 상태 RESOLVED 변경 + post.softDelete()
        reportService.resolveReport(reportId);
        return ResponseEntity.ok().build();
    }

    // 신고 반려 (허위 신고 등 처리)
    @PostMapping("/reports/{reportId}/reject")
    public ResponseEntity<Void> rejectReport(@PathVariable Long reportId,
                                             Authentication authentication) {
        getLoginCustomUser(authentication);
        // 상태 REJECTED 변경
        reportService.rejectReport(reportId);
        return ResponseEntity.ok().build();
    }


    // 사용자 연동
    private CustomUserDetails getLoginCustomUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedAccessException("로그인이 필요합니다.");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails;
        }

        throw new UnauthorizedAccessException("인증된 사용자 정보가 올바르지 않습니다.");
    }

}
