package com.hometalk.onepass.community.controller;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.auth.entity.Household;
import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.community.enums.MarketStatus;
import com.hometalk.onepass.community.exception.UnauthorizedAccessException;
import com.hometalk.onepass.community.repository.TagRepository;
import com.hometalk.onepass.community.service.PostActionService;
import com.hometalk.onepass.community.service.PostService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostActionController {

    private final PostActionService postActionService;
    private final PostService postService;

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
