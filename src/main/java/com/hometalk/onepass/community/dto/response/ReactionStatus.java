package com.hometalk.onepass.community.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReactionStatus {
    private Long id;
    private boolean liked;
    private boolean disliked;
    private int likeCount;
    private int dislikeCount;
}
