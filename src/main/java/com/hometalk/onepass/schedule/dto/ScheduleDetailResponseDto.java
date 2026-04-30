package com.hometalk.onepass.schedule.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDetailResponseDto {

    private Long id;
    private Long noticeId;      // 연결된 공지 (없으면 null)
    private String title;
    private String info;
    private String location;
    private String referenceUrl;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String badge;       // 실제 표시 배지 (공지 배지 우선)
}