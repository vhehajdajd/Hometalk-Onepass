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
    boolean existsByPostAndUserAndType(Post post, User user, ReactionType type);

    Optional<PostReaction> findByPostAndUserAndType(Post post, User user, ReactionType type);

    void deleteByPostAndUserAndType(Post post, User user, ReactionType type);

    @Query("SELECT COUNT(r) FROM PostReaction r WHERE r.post = :post AND r.type = :type")
    long countByPostAndType(@Param("post") Post post, @Param("type") ReactionType type);
}
