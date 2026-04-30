package com.hometalk.onepass.schedule.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleCalResponseDto {

    private Long id;
    private String title;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Long noticeId;   // 연결된 공지 ID (없으면 null)
    private String badge;    // 공지 배지 우선, 없으면 독립 일정 배지

}