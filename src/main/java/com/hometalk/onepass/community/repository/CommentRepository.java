package com.hometalk.onepass.community.repository;

import com.hometalk.onepass.community.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    // 게시글 ID로 댓글 리스트 찾고, 생성일 순으로 정렬(먼저 작성한 게 상단)
    List<Comment> findAllByPostIdOrderByCreatedAtAsc(Long postId);
}
