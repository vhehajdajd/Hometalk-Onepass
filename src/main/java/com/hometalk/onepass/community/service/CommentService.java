package com.hometalk.onepass.community.service;

import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.auth.repository.UserRepository;
import com.hometalk.onepass.community.dto.request.CommentRqDTO;
import com.hometalk.onepass.community.dto.response.CommentRsDTO;
import com.hometalk.onepass.community.entity.Comment;
import com.hometalk.onepass.community.entity.Post;
import com.hometalk.onepass.community.exception.PostNotFoundException;
import com.hometalk.onepass.community.exception.UnauthorizedAccessException;
import com.hometalk.onepass.community.repository.CommentRepository;
import com.hometalk.onepass.community.repository.PostRepository;
import com.hometalk.onepass.community.validator.CommentValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;        // 게시글 확인용
    private final UserRepository userRepository;        // 작성자 확인용`
    private final CommentValidator validator;

    // 댓글 목록 (R)
    @Transactional(readOnly = true)
    public List<CommentRsDTO> findAllByPostId(Long postId) {
        return commentRepository.findAllByPostIdOrderByCreatedAtAsc(postId)
                .stream()
                .map(CommentRsDTO::from)
                .toList();
    }

    // 댓글 작성 (C)
    @Transactional
    public void saveComment(Long postId, Long userId, CommentRqDTO dto) {
        // 게시글 존재 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId, "게시글이 존재하지 않습니다."));
        if (userId == null) {
            throw new UnauthorizedAccessException("로그인이 필요합니다.");
        }
        // 작성자 확인
        User writer = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 엔티티 생성
        Comment comment = Comment.builder()
                .post(post)
                .writer(writer)
                .content(dto.getContent())
                .build();
        commentRepository.save(comment);
    }

    // 댓글 수정 (U)
    @Transactional
    public void updateComment(Long commentId, Long userId, CommentRqDTO dto) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 댓글이 없습니다."));

        // 본인 확인
        validator.validateOwner(comment, userId);

        // 엔티티의 변경 감지(Dirty Checking)를 이용한 수정
        comment.updateContent(dto.getContent());
    }


    // 댓글 삭제 (D)
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 댓글이 없습니다."));
        // 본인 확인
        validator.validateOwner(comment, userId);
        commentRepository.delete(comment);
    }
}
