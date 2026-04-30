package com.hometalk.onepass.community.controller;

/*
    게시글 상태 변경, 상단 고정, 조회수, 좋아요/추천 기능
 */

import com.hometalk.onepass.community.enums.MarketStatus;
import com.hometalk.onepass.community.service.PostActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostActionController {
    private final PostActionService postActionService;

    // 1. 공지 고정 토글 (관리자용)
    @PostMapping("/{postId}/pin")
    public ResponseEntity<Void> togglePin(@PathVariable Long postId) {
        System.out.println("요청 성공");
        // [임시] adminId는 서비스 내부나 세션에서 처리하도록 변경
        Long tempAdminId = 1L;
        postActionService.togglePin(postId, tempAdminId);
        return ResponseEntity.ok().build(); // 200 OK만 반환
    }

    // 2. 나눔 상태 변경 (작성자용)
    @PostMapping("/{postId}/market-status")
    public String updateMarketStatus(@PathVariable Long postId, @RequestParam MarketStatus status) {
        postActionService.updateMarketStatus(postId, status);
        return "redirect:/community/post/" + postId;
    }

    // 3. 관리자 게시글 숨김
    @PostMapping("/{postId}/hide")
    public ResponseEntity<Void> hidePost(@PathVariable Long postId) {
        postActionService.hidePost(postId);
        return ResponseEntity.ok().build();
    }
}
