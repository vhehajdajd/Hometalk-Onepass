package com.hometalk.onepass.community.controller;

import com.hometalk.onepass.community.dto.CommunityPostResponseDTO;
import com.hometalk.onepass.community.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community")
public class PostApiController {

    private final PostService postService;

    @GetMapping("/recent")
    public ResponseEntity<List<CommunityPostResponseDTO>> getRecentPosts() {
        List<CommunityPostResponseDTO> recentPosts = postService.getRecentPosts();
        // 데이터가 없어도 빈 리스트([])를 반환하여 프론트엔드 에러 방지
        return ResponseEntity.ok(recentPosts);
    }

    @GetMapping("/popular")
    public ResponseEntity<List<CommunityPostResponseDTO>> getPopularPosts() {
        return ResponseEntity.ok(postService.getPopularPosts());
    }
}
