package com.hometalk.onepass.community.service;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.auth.repository.UserRepository;
import com.hometalk.onepass.community.dto.request.PostRequestDTO;
import com.hometalk.onepass.community.dto.response.ReactionStatus;
import com.hometalk.onepass.community.entity.PostReaction;
import com.hometalk.onepass.community.enums.MarketStatus;
import com.hometalk.onepass.community.entity.Post;
import com.hometalk.onepass.community.enums.PostStatus;
import com.hometalk.onepass.community.enums.ReactionType;
import com.hometalk.onepass.community.enums.TradeStatus;
import com.hometalk.onepass.community.exception.PostNotFoundException;
import com.hometalk.onepass.community.exception.UnauthorizedAccessException;
import com.hometalk.onepass.community.exception.UserNotFoundException;
import com.hometalk.onepass.community.repository.PostReactionRepository;
import com.hometalk.onepass.community.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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
                .orElseThrow(() -> new PostNotFoundException(postId, "게시글이 존재하지 않습니다."));
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
                .orElseThrow(() -> new PostNotFoundException(postId, "게시글이 존재하지 않습니다."));
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
                .orElseThrow(() -> new PostNotFoundException(postId, "게시글이 존재하지 않습니다."));
        // 3. 상태 반전 (true -> false / false -> true)
        post.togglePinned();
        return post.isPinned();
    }

    // 임시저장
    @Transactional
    public void saveAsDraft(Long postId, PostRequestDTO dto) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId, "게시글이 존재하지 않습니다."));
        post.update(dto.getTitle(), dto.getContent(), post.getCategory(), dto.getPostStatus());
        post.updateStatus(PostStatus.DRAFT);
    }


    // 관리자 '숨김' 처리
    @Transactional
    public void hidePost(Long postId, CustomUserDetails user) {
        if (user == null || user.getRole() != User.UserRole.ADMIN) throw  new UnauthorizedAccessException("관리자 권한 필요");
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId, "게시글이 존재하지 않습니다."));
        post.updateStatus(PostStatus.HIDDEN);
    }

    // 관리자 숨김 '해제' 처리
    @Transactional
    public void unhidePost(Long postId, CustomUserDetails user) {
        if (user == null || user.getRole() != User.UserRole.ADMIN) {
            throw new UnauthorizedAccessException("관리자 권한이 필요합니다.");
        }
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId, "게시글이 존재하지 않습니다."));
        post.updateStatus(PostStatus.ACTIVE);
    }

    // 조회수 증가
    @Transactional
    public void increaseViewCount(Long postId, Long currentUserId, List<Long> viewedPosts) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId, "게시글이 존재하지 않습니다."));
        // 1. 본인 글 제외 로직
        if (currentUserId != null && post.getWriter().getId().equals(currentUserId)) return;
        // 2. 중복 조회 방지
        if (viewedPosts != null && viewedPosts.contains(postId)) return;
        // 3. 위 조건들을 통과하면 조회수 증가
        post.addViewCount();
        // 4. 읽은 목록에 추가
        if (viewedPosts != null) {
            viewedPosts.add(postId);
        }
    }

    // 좋아요/싫어요
    public ReactionStatus getReactionStatus(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId, "게시글이 존재하지 않습니다."));

        boolean liked = false;
        boolean disliked = false;

        if (userId != null) {
            User loginUser = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException(userId, post.getCategory().getBoard().getCode()));
            liked = postReactionRepository.findByPostAndUserAndType(post, loginUser, ReactionType.LIKE).isPresent();
            disliked = postReactionRepository.findByPostAndUserAndType(post, loginUser, ReactionType.DISLIKE).isPresent();
        }

        return new ReactionStatus(post.getId(), liked, disliked, post.getLikeCount(), post.getDislikeCount());
    }


    @Transactional
    public ReactionStatus toggleReactionAndGetStatus(Long postId, CustomUserDetails user, ReactionType type) {
        if (user == null) throw new UnauthorizedAccessException("로그인이 필요합니다.");
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId, "게시글이 존재하지 않습니다."));
        User loginUser = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new UserNotFoundException(user.getUserId(), post.getCategory().getBoard().getCode()));

        if (type == ReactionType.DISLIKE && !"debate".equals(post.getCategory().getCode())) {
            throw new IllegalArgumentException("토론 게시글에서만 싫어요 가능합니다.");
        }

        ReactionType oppositeType = (type == ReactionType.LIKE) ? ReactionType.DISLIKE : ReactionType.LIKE;

        postReactionRepository.findByPostAndUserAndType(post, loginUser, oppositeType)
                .ifPresent(oppositeReaction -> {
                    postReactionRepository.delete(oppositeReaction);
                    if (oppositeType == ReactionType.LIKE) post.decreaseLikeCount();
                    else post.decreaseDislikeCount();
                });

        // 본 반응 토글 처리 및 변수로 상태 기억
        final boolean[] isTargetAdded = {false};

        postReactionRepository.findByPostAndUserAndType(post, loginUser, type)
                .ifPresentOrElse(existing -> {
                    postReactionRepository.delete(existing);
                    if (type == ReactionType.LIKE) post.decreaseLikeCount();
                    else post.decreaseDislikeCount();
                    isTargetAdded[0] = false;
                }, () -> {
                    postReactionRepository.save(new PostReaction(post, loginUser, type));
                    if (type == ReactionType.LIKE) post.increaseLikeCount();
                    else post.increaseDislikeCount();
                    isTargetAdded[0] = true;
                });

        // 메모리에 캐싱된 데이터로 결과 조립
        boolean finalLiked = (type == ReactionType.LIKE) ? isTargetAdded[0] : false;
        boolean finalDisliked = (type == ReactionType.DISLIKE) ? isTargetAdded[0] : false;

        return new ReactionStatus(post.getId(), finalLiked, finalDisliked, post.getLikeCount(), post.getDislikeCount());
    }
}
