package com.hometalk.onepass.community.dto.request;

import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.community.entity.*;
import com.hometalk.onepass.community.enums.MarketStatus;
import com.hometalk.onepass.community.enums.PostStatus;
import com.hometalk.onepass.community.enums.TradeStatus;
import com.hometalk.onepass.community.enums.TradeType;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

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
    private String categoryCode;

    private Long writerId;              // Member Entity 구현 전까지 유지
    private PostStatus postStatus;      // 게시글 상태 변경
    private MarketStatus marketStatus;
    private TradeStatus tradeStatus;
    private TradeType tradeType;

    private boolean pinned;             // 관리자 상단 고정

    private List<String> tags;

    public List<String> getTags() {
        return tags == null ? new ArrayList<>() : tags;
    }

    public Post toEntity(Category category, Board board, User writer) {
        return Post.builder().title(this.title)
                .content(this.content).pinned(this.pinned)
                .postStatus(this.postStatus != null ? this.postStatus : PostStatus.ACTIVE)
                .marketStatus(this.marketStatus!= null ? this.marketStatus : MarketStatus.SHARED)
                .tradeStatus(this.tradeStatus != null ? this.tradeStatus : TradeStatus.SELLING)
                .tradeType(this.tradeType)
                .writer(writer)
                .category(category)
                .board(board)
                .hasImage(false)
                .build();
    }
}
