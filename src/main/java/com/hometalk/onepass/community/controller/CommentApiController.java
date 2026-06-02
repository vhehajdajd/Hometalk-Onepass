package com.hometalk.onepass.community.controller;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.community.dto.request.CommentRqDTO;
import com.hometalk.onepass.community.dto.response.CommentRsDTO;
import com.hometalk.onepass.community.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/community/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentApiController {

    private final CommentService commentService;

    @GetMapping
    public List<CommentRsDTO> getComments(@PathVariable Long postId,
                                          Authentication authentication) {
        Long userId = getLoginUserIdOrNull(authentication);
        return commentService.findAllByPostId(postId, userId);
    }

    @PostMapping
    public List<CommentRsDTO> saveComment(@PathVariable Long postId,
                                          @RequestBody CommentRqDTO dto,
                                          Authentication authentication) {
        Long userId = getLoginUserId(authentication);
        commentService.saveComment(postId, userId, dto);
        return commentService.findAllByPostId(postId, userId);
    }

    @PostMapping("/{commentId}/edit")
    public List<CommentRsDTO> updateComment(@PathVariable Long postId,
                                            @PathVariable Long commentId,
                                            @RequestBody CommentRqDTO dto,
                                            Authentication authentication) {
        Long userId = getLoginUserId(authentication);
        commentService.updateComment(commentId, userId, dto);
        return commentService.findAllByPostId(postId, userId);
    }

    @PostMapping("/{commentId}/delete")
    public List<CommentRsDTO> deleteComment(@PathVariable Long postId,
                                            @PathVariable Long commentId,
                                            Authentication authentication) {
        Long userId = getLoginUserId(authentication);
        commentService.deleteComment(commentId, userId);
        return commentService.findAllByPostId(postId, userId);
    }

    private Long getLoginUserIdOrNull(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUserId();
        }

        return null;
    }

    private Long getLoginUserId(Authentication authentication) {
        Long userId = getLoginUserIdOrNull(authentication);

        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        return userId;
    }
}