package com.hometalk.onepass.community.controller;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.auth.entity.Household;
import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.community.dto.request.PostRequestDTO;
import com.hometalk.onepass.community.dto.response.PostListResponse;
import com.hometalk.onepass.community.dto.response.ReactionStatus;
import com.hometalk.onepass.community.entity.Post;
import com.hometalk.onepass.community.enums.MarketStatus;
import com.hometalk.onepass.community.enums.PostStatus;
import com.hometalk.onepass.community.enums.ReactionType;
import com.hometalk.onepass.community.enums.TradeStatus;
import com.hometalk.onepass.community.exception.UnauthorizedAccessException;
import com.hometalk.onepass.community.service.FileService;
import com.hometalk.onepass.community.service.PostActionService;
import com.hometalk.onepass.community.service.PostService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resident")
@RequiredArgsConstructor
public class CommunityApiController {

    private final PostActionService postActionService;
    private final PostService postService;
    private final FileService fileService;

    @Value("${file.upload.path}")
    private String uploadPath;

    // 임시저장
    @PostMapping("/{boardCode}/save-temp")
    public ResponseEntity<?> saveTempApi(@PathVariable("boardCode") String boardCode,
                                         @ModelAttribute PostRequestDTO dto,
                                         Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }
        dto.setPostStatus(PostStatus.DRAFT);
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long id = postService.postSave(boardCode, dto, userDetails.getUserId());

        return ResponseEntity.ok(Map.of(
                "id", id,
                "message", "게시글이 임시저장되었습니다."
        ));
    }

    // 임시저장 개수
    @GetMapping("/{boardCode}/temp-count")
    public int getTempCount(@PathVariable("boardCode") String boardCode, Authentication authentication) {
        if (authentication == null) return 0;
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();;
        return postService.getTempPostCount(boardCode, userDetails.getUserId());
    }

    // 임시저장 목록
    @GetMapping("/{boardCode}/temp-list")
    public List<PostListResponse> getTempPosts(@PathVariable("boardCode") String boardCode,
                                               Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 필요");
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return postService.getTempPosts(boardCode, userDetails.getUserId());
    }

    // 임시저장글 삭제
    @PostMapping("/{boardCode}/delete-temp/{id}")
    public ResponseEntity<String> deleteTemp(@PathVariable("boardCode") String boardCode,
                                             @PathVariable Long id,
                                             Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 필요");
            }

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            postService.deletePost(id, userDetails.getUserId(), boardCode);

            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Fail");
        }
    }

    // 이미지 파일 업로드
    @PostMapping("/image-upload")
    public Map<String, String> uploadImage(@RequestParam("file") MultipartFile file) {
        String storeFileName = fileService.storeFile(file);
        Map<String, String> result = new HashMap<>();
        result.put("url", "/uploads/" + storeFileName);
        return result;
    }

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

    @PostMapping("/{postId}/like")
    public ResponseEntity<?> toggleLike(@PathVariable Long postId,
                                        Authentication authentication) {
        try {
            CustomUserDetails user = getLoginCustomUser(authentication);
            ReactionStatus status = postActionService.toggleReactionAndGetStatus(postId, user, ReactionType.LIKE);
            return ResponseEntity.ok(status);
        } catch (IllegalArgumentException | UnauthorizedAccessException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("code", "C400", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("code", "C999", "message", "서버 내부 오류가 발생했습니다"));
        }
    }

    @PostMapping("/{postId}/dislike")
    public ResponseEntity<?> toggleDislike(@PathVariable Long postId,
                                           Authentication authentication) {
        try {
            CustomUserDetails user = getLoginCustomUser(authentication);
            ReactionStatus status = postActionService.toggleReactionAndGetStatus(postId, user, ReactionType.DISLIKE);
            return ResponseEntity.ok(status);
        } catch (IllegalArgumentException | UnauthorizedAccessException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("code", "C400", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("code", "C999", "message", "서버 내부 오류가 발생했습니다"));
        }
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
