package com.hometalk.onepass.community.controller;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.community.dto.request.CommentRqDTO;
import com.hometalk.onepass.community.service.CommentService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/community/{boardCode}/{categoryCode}/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PersistenceContext
    private EntityManager entityManager;

    @PostMapping
    public String saveComment(@PathVariable String boardCode,
                              @PathVariable String categoryCode,
                              @PathVariable Long postId,
                              @ModelAttribute CommentRqDTO commentRqDTO,
                              RedirectAttributes redirectAttributes,
                              Authentication authentication) {

        Long userId = getLoginUserId(authentication);

        commentService.saveComment(postId, userId, commentRqDTO);

        redirectAttributes.addFlashAttribute("successMessage", "댓글이 등록되었습니다.");
        log.info("입력된 댓글 내용: {}", commentRqDTO.getContent());

        return String.format("redirect:/community/%s/%s/%d", boardCode, categoryCode, postId);
    }

    @PostMapping("/{commentId}/edit")
    public String updateComment(@PathVariable String boardCode,
                                @PathVariable String categoryCode,
                                @PathVariable Long postId,
                                @PathVariable Long commentId,
                                @ModelAttribute CommentRqDTO commentRqDTO,
                                Authentication authentication) {

        Long userId = getLoginUserId(authentication);

        commentService.updateComment(commentId, userId, commentRqDTO);

        return String.format("redirect:/community/%s/%s/%d", boardCode, categoryCode, postId);
    }

    @PostMapping("/{commentId}/delete")
    public String deleteComment(@PathVariable String boardCode,
                                @PathVariable String categoryCode,
                                @PathVariable Long postId,
                                @PathVariable Long commentId,
                                RedirectAttributes redirectAttributes,
                                Authentication authentication) {

        Long userId = getLoginUserId(authentication);

        commentService.deleteComment(commentId, userId);

        redirectAttributes.addFlashAttribute("successMessage", "댓글이 삭제되었습니다.");
        return String.format("redirect:/community/%s/%s/%d", boardCode, categoryCode, postId);
    }

    private Long getLoginUserId(Authentication authentication) {
        return getLoginUser(authentication).getId();
    }

    private User getLoginUser(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
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