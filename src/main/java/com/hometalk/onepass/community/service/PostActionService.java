package com.hometalk.onepass.community.service;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.auth.repository.UserRepository;
import com.hometalk.onepass.community.dto.request.PostRequestDTO;
import com.hometalk.onepass.community.entity.PostReaction;
import com.hometalk.onepass.community.enums.MarketStatus;
import com.hometalk.onepass.community.entity.Post;
import com.hometalk.onepass.community.enums.PostStatus;
import com.hometalk.onepass.community.enums.ReactionType;
import com.hometalk.onepass.community.enums.TradeStatus;
import com.hometalk.onepass.community.exception.UnauthorizedAccessException;
import com.hometalk.onepass.community.repository.PostReactionRepository;
import com.hometalk.onepass.community.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostActionService {

    private final PostRepository postRepository;
    private final PostReactionRepository postReactionRepository;
    private final UserRepository userRepository;

    // 나눔 상태 변경
    @Transactional
    public void updateMarketStatus(Long postId, CustomUserDetails user, MarketStatus status) {
        if (user == null) {
            throw new UnauthorizedAccessException("로그인이 필요합니다.");
        }
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 없습니다."));
        if (!post.getWriter().getId().equals(user.getUserId())) {
            throw new UnauthorizedAccessException("작성자만 상태 변경 가능합니다.");
        }
        post.updateMarketStatus(status);
    }

    // 거래 상태 변경
    @Transactional
    public void updateTradeStatus(Long postId, CustomUserDetails user, TradeStatus status) {
        if (user == null) {
            throw new UnauthorizedAccessException("로그인이 필요합니다.");
        }
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 없습니다."));
        if (!post.getWriter().getId().equals(user.getUserId())) {
            throw new UnauthorizedAccessException("작성자만 상태 변경 가능합니다.");
        }
        post.updateTradeStatus(status);
    }

    // 상단 고정
    @Transactional
    public boolean togglePin(Long postId, CustomUserDetails user) {
        // 0. 로그인 체크
        if (user == null) {
            throw new UnauthorizedAccessException("로그인이 필요합니다.");
        }
        // 1. 관리자 권한 체크
        if (user.getRole() != User.UserRole.ADMIN) {
            throw new UnauthorizedAccessException("관리자 권한이 필요합니다.");
        }
        // 2. 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 없습니다."));
        // 3. 상태 반전 (true -> false / false -> true)
        post.togglePinned();
        return post.isPinned();
    }

    // 임시저장
    @Transactional
    public void saveAsDraft(Long postId, PostRequestDTO dto) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 없습니다."));
        post.update(dto.getTitle(), dto.getContent(), post.getCategory(), dto.getPostStatus());
        post.updateStatus(PostStatus.DRAFT);
    }


    // 관리자 '숨김' 처리
    @Transactional
    public void hidePost(Long postId, CustomUserDetails user) {
        if (user == null || user.getRole() != User.UserRole.ADMIN) throw  new UnauthorizedAccessException("관리자 권한 필요");
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음"));
        post.updateStatus(PostStatus.HIDDEN);
    }

    // 관리자 숨김 '해제' 처리
    @Transactional
    public void unhidePost(Long postId, CustomUserDetails user) {
        if (user == null || user.getRole() != User.UserRole.ADMIN) {
            throw new UnauthorizedAccessException("관리자 권한이 필요합니다.");
        }
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 없습니다."));
        post.updateStatus(PostStatus.ACTIVE);
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

    // 좋아요
    @Transactional
    public boolean toggleReaction(Long postId, CustomUserDetails user, ReactionType type) {
        if (user == null) throw new UnauthorizedAccessException("로그인이 필요합니다.");

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 없습니다."));

        User loginUser = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 1. 반대 타입이 존재하면 삭제
        ReactionType opposite = (type == ReactionType.LIKE) ? ReactionType.DISLIKE : ReactionType.LIKE;
        postReactionRepository.findByPostAndUserAndType(post, loginUser, opposite)
                .ifPresent(r -> {
                    postReactionRepository.delete(r);
                    if (opposite == ReactionType.LIKE) post.decreaseLikeCount();
                    else post.decreaseDislikeCount();
                });

        // 2. 같은 타입 토글
        return postReactionRepository.findByPostAndUserAndType(post, loginUser, type)
                .map(r -> {
                    postReactionRepository.delete(r);
                    if (type == ReactionType.LIKE) post.decreaseLikeCount();
                    else post.decreaseDislikeCount();
                    return false;
                })
                .orElseGet(() -> {
                    postReactionRepository.save(new PostReaction(post, loginUser, type));
                    if (type == ReactionType.LIKE) post.increaseLikeCount();
                    else post.increaseDislikeCount();
                    return true;
                });
    }

    @Transactional(readOnly = true)
    public int getReactionCount(Long postId, ReactionType type) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 없습니다."));
        return Math.toIntExact(postReactionRepository.countByPostAndType(post, type));
    }

    public boolean toggleLike(Long postId, CustomUserDetails user) {
        return toggleReaction(postId, user, ReactionType.LIKE);
    }

    public boolean toggleDislike(Long postId, CustomUserDetails user) {
        return toggleReaction(postId, user, ReactionType.DISLIKE);
    }

    public int getLikeCount(Long postId) {
        return getReactionCount(postId, ReactionType.LIKE);
    }

    public int getDislikeCount(Long postId) {
        return getReactionCount(postId, ReactionType.DISLIKE);
    }
}
