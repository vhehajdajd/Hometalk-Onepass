package com.hometalk.onepass.notice.dto;


import com.hometalk.onepass.notice.entity.Badge;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class NoticeDetailResponseDto {

    private Long id;    // 이전/다음글 이동에서 필요

    private String title;

    private String content;

    private int viewCount;

    private Badge badge;  // 중요, 시설, 안전, 일반

    private Boolean isPinned;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}