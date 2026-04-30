package com.hometalk.onepass.community.dto.request;

import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.community.entity.*;
import com.hometalk.onepass.community.enums.MarketStatus;
import com.hometalk.onepass.community.enums.PostStatus;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostRequestDTO {
    private Long id;        // 수정 시 필요
    private String title;
    private String content;
    private Long categoryId;

    private Long writerId;              // Member Entity 구현 전까지 유지
    private PostStatus postStatus;      // 게시글 상태 변경
    private MarketStatus marketStatus;
    private boolean pinned;             // 관리자 상단 고정

    public Post toEntity(Category category, Board board, User writer) {
        return Post.builder().title(this.title)
                .content(this.content).pinned(this.pinned)
                .postStatus(this.postStatus != null ? this.postStatus : PostStatus.ACTIVE)
                .marketStatus(this.marketStatus!= null ? this.marketStatus : MarketStatus.SHARED)
                .writer(writer)
                .category(category)
                .board(board)
                .build();
    }
}
