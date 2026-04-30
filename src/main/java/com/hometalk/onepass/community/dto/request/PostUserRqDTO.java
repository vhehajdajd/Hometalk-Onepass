package com.hometalk.onepass.community.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostUserRqDTO {
    private Long postId;
    private String marketStatus;
    private String postStatus;
    private boolean isPinned;
}
