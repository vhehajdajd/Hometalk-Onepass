package com.hometalk.onepass.community.service;

import com.hometalk.onepass.community.dto.request.PostRequestDTO;
import com.hometalk.onepass.community.enums.MarketStatus;
import com.hometalk.onepass.community.entity.Post;
import com.hometalk.onepass.community.enums.PostStatus;
import com.hometalk.onepass.community.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostActionService {

    private final PostRepository postRepository;

    public void updateMarketStatus(Long postId, MarketStatus status) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 없습니다."));
        post.updateMarketStatus(status);
    }

    @Transactional
    public boolean togglePin(Long id, Long adminId) {
        // 1. 게시글 조회
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 없습니다."));
        // 2. 관리자 권한 확인 (임시로 adminId가 1L인 경우만 허용하거나, 나중에 Role로 체크)
        // 지금은 테스트를 위해 간단히 처리
        if (adminId == null || adminId != 1L) {
            throw new IllegalStateException("관리자 권한이 필요합니다.");
        }
        // 3. 상태 반전 (true -> false / false -> true)
        post.togglePinned();
        return post.isPinned();
    }

    public void saveAsDraft(Long postId, PostRequestDTO dto) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 없습니다."));
        post.update(dto);
        post.updateStatus(PostStatus.DRAFT);
    }


    // 관리자 '숨김' 처리
    @Transactional
    public void hidePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음"));
        post.updateStatus(PostStatus.HIDDEN);
    }

    // 조회수 증가
    @Transactional
    public void increaseViewCount(Long postId, Long currentUserId, List<Long> viewedPosts) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 없습니다."));

        // 1. 본인 글 제외 로직
        // 작성자가 로그인 유저와 같다면 조회수를 올리지 않고 즉시 종료
        if (currentUserId != null && post.getWriter().getId().equals(currentUserId)) return;

        // 2. 중복 조회 방지
        if (viewedPosts != null && viewedPosts.contains(postId)) return;

        // 3. 위 조건들을 통과하면 조회수 증가
        post.addViewCount();

        // 4. 읽은 목록에 추가 (이건 컨트롤러/서비스 단에서 세션에 담아줘야 함)
        if (viewedPosts != null) {
            viewedPosts.add(postId);
        }
    }
}
