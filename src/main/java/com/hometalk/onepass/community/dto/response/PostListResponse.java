package com.hometalk.onepass.community.dto.response;

import com.hometalk.onepass.community.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PostListResponse {
    private Long id;
    private String title;
    private boolean pinned;
    private String boardName;
    private String categoryName;
    private String categoryCode;
    private String writer;
    private LocalDateTime createdAt;
    private int viewCount;
    private int commentCount;

    public PostListResponse(Post post) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.pinned = post.isPinned();
        this.boardName = post.getCategory().getBoard().getName();
        this.categoryName = post.getCategory().getName();
        this.categoryCode = post.getCategory().getCode();
        this.writer = post.getWriter().getNickname();
        this.createdAt = post.getCreatedAt();
        this.viewCount = post.getViewCount();
        this.commentCount = post.getComments().size();
    }

    public String getCategoryCode() {
        return categoryCode;
    }
}
