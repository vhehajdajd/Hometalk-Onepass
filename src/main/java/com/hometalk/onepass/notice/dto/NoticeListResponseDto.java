package com.hometalk.onepass.notice.dto;

import com.hometalk.onepass.notice.entity.Badge;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class NoticeListResponseDto {

    private Long id;
    private String title;
    private Badge badge;
    private Boolean isPinned;
    private int viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
