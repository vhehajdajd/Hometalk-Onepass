package com.hometalk.onepass.community.controller;

/*
    게시글 상태 변경, 상단 고정, 조회수, 좋아요/추천 기능
 */

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.community.enums.MarketStatus;
import com.hometalk.onepass.community.exception.UnauthorizedAccessException;
import com.hometalk.onepass.community.service.PostActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostActionController {
    private final PostActionService postActionService;

    // 1. 공지 고정 토글 (관리자용)
    @PostMapping("/{postId}/pin")
    public ResponseEntity<Void> togglePin(@PathVariable Long postId,
                                          @AuthenticationPrincipal CustomUserDetails user) {
        System.out.println("요청 성공");
        postActionService.togglePin(postId, user);
        return ResponseEntity.ok().build(); // 200 OK만 반환
    }

    // 2. 나눔 상태 변경 (작성자용)
    @PostMapping("/{postId}/status")
    public String updateMarketStatus(@PathVariable Long postId,
                                     @RequestBody java.util.Map<String, String> request,
                                     @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) {
            throw new UnauthorizedAccessException("로그인이 필요합니다.");
        }
        MarketStatus marketStatus = MarketStatus.valueOf(request.get("marketStatus"));

        postActionService.updateMarketStatus(postId, user, marketStatus);
        return "redirect:/community/post/" + postId;
    }

    // 3. 관리자 게시글 숨김
    @PostMapping("/{postId}/hide")
    public ResponseEntity<Void> hidePost(@PathVariable Long postId,
                                         @AuthenticationPrincipal CustomUserDetails user) {

        postActionService.hidePost(postId, user);
        return ResponseEntity.ok().build();
    }
}
