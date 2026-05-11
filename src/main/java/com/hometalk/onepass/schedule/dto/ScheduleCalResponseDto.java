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
    private Long noticeId;
    private String badge;
    private Long repeatGroupId;
    private String info;      // 일정 설명
    private String location;  // 장소

}