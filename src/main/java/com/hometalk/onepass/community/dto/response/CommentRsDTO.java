package com.hometalk.onepass.community.dto.response;

import com.hometalk.onepass.community.entity.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CommentRsDTO {
    private Long id;
    private String content;

    private Long writerId;
    private String nickname;
    private LocalDateTime createdAt;

    private boolean editable;

    public static CommentRsDTO from(Comment comment) {
        return from(comment, null);
    }

    public static CommentRsDTO from(Comment comment, Long loginUserId) {
        return CommentRsDTO.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .writerId(comment.getWriter().getId())
                .nickname(comment.getWriter().getNickname())
                .createdAt(comment.getCreatedAt())
                .editable(loginUserId != null && comment.getWriter().getId().equals(loginUserId))
                .build();
    }
}
