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

    public static CommentRsDTO from(Comment comment) {
        return CommentRsDTO.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .writerId(comment.getWriter().getId())
                .nickname(comment.getWriter().getNickname())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
