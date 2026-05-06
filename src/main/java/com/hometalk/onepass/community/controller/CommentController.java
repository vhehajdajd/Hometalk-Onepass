package com.hometalk.onepass.community.controller;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.community.dto.request.CommentRqDTO;
import com.hometalk.onepass.community.dto.response.CommentRsDTO;
import com.hometalk.onepass.community.dto.response.PostResponseDTO;
import com.hometalk.onepass.community.dto.response.PostUserRsDTO;
import com.hometalk.onepass.community.service.CommentService;
import com.hometalk.onepass.community.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
/*
    댓글 저장/수정/삭제
 */
@Slf4j
@Controller
@RequestMapping("/community/{boardCode}/{categoryCode}/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    // 댓글 작성
    @PostMapping
    public String saveComment(@PathVariable String boardCode,
                              @PathVariable String categoryCode,
                              @PathVariable Long postId,
                              @ModelAttribute CommentRqDTO commentRqDTO,
                              RedirectAttributes redirectAttributes,
                              @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) {
            return "redirect:/login";
        }
        Long userId = user.getUserId();

        // 댓글 저장
        commentService.saveComment(postId, userId, commentRqDTO);

        redirectAttributes.addFlashAttribute("successMessage", "댓글이 등록되었습니다.");
        log.info("입력된 댓글 내용: {}", commentRqDTO.getContent());
        // 처리가 끝나면 다시 상세 페이지로 리다이렉트
        return String.format("redirect:/community/%s/%s/%d", boardCode, categoryCode, postId);
    }


    // 댓글 수정
    @PostMapping("/{commentId}/edit")
    public String updateComment(@PathVariable String boardCode,
                                @PathVariable String categoryCode,
                                @PathVariable Long postId,
                                @PathVariable Long commentId,
                                @ModelAttribute CommentRqDTO commentRqDTO,
                                @AuthenticationPrincipal CustomUserDetails user) {

        if (user == null) {
            return "redirect:/login";
        }
        commentService.updateComment(commentId, user.getUserId(), commentRqDTO);

        return String.format("redirect:/community/%s/%s/%d", boardCode, categoryCode, postId);
    }


    // 댓글 삭제
    @PostMapping("/{commentId}/delete")
    public String deleteComment(@PathVariable String boardCode,
                                @PathVariable String categoryCode,
                                @PathVariable Long postId,
                                @PathVariable Long commentId,
                                RedirectAttributes redirectAttributes,
                                @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) {
            return "redirect:/login";
        }
        commentService.deleteComment(commentId, user.getUserId());

        redirectAttributes.addFlashAttribute("successMessage", "댓글이 삭제되었습니다.");
        return String.format("redirect:/community/%s/%s/%d", boardCode, categoryCode, postId);
    }

}
