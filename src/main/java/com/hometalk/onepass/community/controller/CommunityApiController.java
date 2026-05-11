package com.hometalk.onepass.community.controller;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.auth.entity.Household;
import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.community.enums.MarketStatus;
import com.hometalk.onepass.community.enums.TradeStatus;
import com.hometalk.onepass.community.exception.UnauthorizedAccessException;
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
import java.util.Map;

@RestController
@RequestMapping("/api/resident")
@RequiredArgsConstructor
public class CommunityApiController {

    private final PostActionService postActionService;
    private final PostService postService;

    @PersistenceContext
    private EntityManager entityManager;

    // 나눔 상태 변경
    @PostMapping("/{postId}/status")
    public ResponseEntity<Void> updateMarketStatus(@PathVariable Long postId,
                                                   @RequestBody java.util.Map<String, String> request,
                                                   Authentication authentication) {

        CustomUserDetails user = getLoginCustomUser(authentication);

        MarketStatus marketStatus = MarketStatus.valueOf(request.get("marketStatus"));
        postActionService.updateMarketStatus(postId, user, marketStatus);

        return ResponseEntity.ok().build();
    }

    // 거래 상태 변경
    @PostMapping("/{postId}/trade/status")
    public ResponseEntity<Void> updateTradeStatus(
            @PathVariable Long postId,
            @RequestBody Map<String, String> request,
            Authentication authentication
    ) {
        CustomUserDetails user = getLoginCustomUser(authentication);

        String value = request.get("tradeStatus");
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("tradeStatus 값이 없습니다.");
        }
        TradeStatus status = TradeStatus.valueOf(value);
        postActionService.updateTradeStatus(postId, user, status);
        return ResponseEntity.ok().build();
    }

    // 태그 자동완성
    @GetMapping("/tags/search")
    public ResponseEntity<List<String>> searchTags(@RequestParam String keyword) {
        List<String> suggestions = postService.searchTags(keyword);
        return ResponseEntity.ok(suggestions);
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

        User user = getLoginUser(authentication);
        Household household = user.getHousehold();

        return new CustomUserDetails(
                user.getId(),
                household != null ? household.getId() : null,
                household != null ? household.getPostNum() : null,
                user.getName(),
                user.getRole(),
                user.getStatus(),
                user.isApprovalNoticeShown(),
                getLoginId(authentication),
                "",
                user.getNickname()
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
