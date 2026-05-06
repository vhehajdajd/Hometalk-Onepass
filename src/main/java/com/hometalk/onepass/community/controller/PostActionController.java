package com.hometalk.onepass.community.controller;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.community.enums.MarketStatus;
import com.hometalk.onepass.community.exception.UnauthorizedAccessException;
import com.hometalk.onepass.community.service.PostActionService;
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

    @PersistenceContext
    private EntityManager entityManager;

    @PostMapping("/{postId}/pin")
    public ResponseEntity<Void> togglePin(@PathVariable Long postId,
                                          Authentication authentication) {

        CustomUserDetails user = getLoginCustomUser(authentication);

        postActionService.togglePin(postId, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{postId}/status")
    public ResponseEntity<Void> updateMarketStatus(@PathVariable Long postId,
                                                   @RequestBody java.util.Map<String, String> request,
                                                   Authentication authentication) {

        CustomUserDetails user = getLoginCustomUser(authentication);

        MarketStatus marketStatus = MarketStatus.valueOf(request.get("marketStatus"));
        postActionService.updateMarketStatus(postId, user, marketStatus);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{postId}/hide")
    public ResponseEntity<Void> hidePost(@PathVariable Long postId,
                                         Authentication authentication) {

        CustomUserDetails user = getLoginCustomUser(authentication);

        postActionService.hidePost(postId, user);
        return ResponseEntity.ok().build();
    }

    private CustomUserDetails getLoginCustomUser(Authentication authentication) {

        User user = getLoginUser(authentication);

        return new CustomUserDetails(
                user.getId(),
                null,
                null,
                user.getName(),
                user.getRole(),
                getLoginId(authentication),
                ""
        );
    }

    private String getLoginId(Authentication authentication) {

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails customUserDetails
                && customUserDetails.getLoginId() != null) {
            return customUserDetails.getLoginId();
        }

        return authentication.getName();
    }

    private User getLoginUser(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedAccessException("로그인이 필요합니다.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails customUserDetails) {
            Long userId = customUserDetails.getUserId();

            if (userId != null) {
                User user = entityManager.find(User.class, userId);

                if (user != null) {
                    return user;
                }
            }
        }

        String loginId = authentication.getName();

        List<User> users = entityManager.createQuery(
                        "select u " +
                                "from LocalAccount la " +
                                "join la.user u " +
                                "where la.loginId = :loginId",
                        User.class
                )
                .setParameter("loginId", loginId)
                .setMaxResults(1)
                .getResultList();

        if (users.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자 정보를 찾을 수 없습니다.");
        }

        return users.get(0);
    }
}