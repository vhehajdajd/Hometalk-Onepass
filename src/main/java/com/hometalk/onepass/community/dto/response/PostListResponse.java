package com.hometalk.onepass.community.dto.response;

import com.hometalk.onepass.community.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    private boolean hasImage;

    private List<String> tags;

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
        this.hasImage = post.getContent() != null && post.getContent().contains("<img");
        if (post.getPostTags() != null && !post.getPostTags().isEmpty()) {
            this.tags = post.getPostTags().stream()
                    .map(pt -> pt.getTag().getName())
                    .collect(Collectors.toList());
        } else {
            this.tags = new ArrayList<>(); // null 대신 빈 리스트
        }
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public boolean isHasImage() {
        return this.hasImage;
    }
}
