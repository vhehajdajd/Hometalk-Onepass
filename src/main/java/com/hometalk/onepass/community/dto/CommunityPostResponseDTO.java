package com.hometalk.onepass.community.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CommunityPostResponseDTO {
    private Long postId;
    private String title;
    private String categoryName;
    private String categoryCode;
}
