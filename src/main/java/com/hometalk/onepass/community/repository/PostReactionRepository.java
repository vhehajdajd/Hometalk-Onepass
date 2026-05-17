package com.hometalk.onepass.community.repository;

import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.community.entity.Post;
import com.hometalk.onepass.community.entity.PostReaction;
import com.hometalk.onepass.community.enums.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostReactionRepository extends JpaRepository<PostReaction, Long> {

    // 특정 게시글에서 특정 사용자 + 타입이 존재하는지 확인 (좋아요/싫어요 여부)
    boolean existsByPostIdAndUserIdAndType(Long postId, Long userId, ReactionType type);

    // 특정 게시글과 사용자 + 타입에 해당하는 PostReaction 조회
    Optional<PostReaction> findByPostAndUserAndType(Post post, User user, ReactionType type);
}
