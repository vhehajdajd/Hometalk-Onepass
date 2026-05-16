package com.hometalk.onepass.community.dto.response;

import com.hometalk.onepass.community.entity.Post;
import com.hometalk.onepass.community.enums.MarketStatus;
import com.hometalk.onepass.community.enums.PostFileType;
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
    private String thumbnailPath;

    private String categoryBgColor;
    private String categoryTextColor;

    private String marketStatus;
    private String marketStatusDescription;

    private String tradeType;
    private String tradeTypeDescription;
    private String tradeStatus;
    private String tradeStatusDescription;

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
        // 대표 썸네일 조회
        if (post.getFiles() != null && !post.getFiles().isEmpty()) {

            post.getFiles().stream()
                    .filter(file -> file.getFileType() == PostFileType.THUMBNAIL)
                    .findFirst()
                    .ifPresent(file -> this.thumbnailPath = file.getFilePath());
        }

        if (post.getCategory() != null) {
            this.categoryName = post.getCategory().getName();
            this.categoryCode = post.getCategory().getCode();
            this.categoryBgColor = post.getCategory().getBgColor();
            this.categoryTextColor = post.getCategory().getTextColor();
        }

        if (post.getPostTags() != null && !post.getPostTags().isEmpty()) {
            this.tags = post.getPostTags().stream()
                    .map(pt -> pt.getTag().getName())
                    .collect(Collectors.toList());
        } else {
            this.tags = new ArrayList<>(); // null 대신 빈 리스트
        }

        // 나눔 게시글 상태
        if ("share".equalsIgnoreCase(post.getCategory().getCode())
                && post.getMarketStatus() != null
                && !post.isPinned()) {
            this.marketStatus = post.getMarketStatus().name();
            this.marketStatusDescription = post.getMarketStatus().getDescription();
        } else {
            this.marketStatus = null;
            this.marketStatusDescription = null;
        }

        // 거래 게시판 상태
        if ("trade".equalsIgnoreCase(post.getCategory().getCode())
                && post.getTradeType() != null
                && post.getTradeStatus() != null
                && !post.isPinned()) {

            this.tradeType = post.getTradeType().name();
            this.tradeTypeDescription = post.getTradeType().getDescription();

            this.tradeStatus = post.getTradeStatus().name();
            this.tradeStatusDescription = post.getTradeStatus().getDescription();

        } else {
            this.tradeType = null;
            this.tradeTypeDescription = null;
            this.tradeStatus = null;
            this.tradeStatusDescription = null;
        }
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public boolean isHasImage() {
        return this.hasImage;
    }
}
