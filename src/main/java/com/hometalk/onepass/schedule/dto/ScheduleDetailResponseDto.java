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
    private Long noticeId;
    private String title;
    private String info;
    private String location;
    private String referenceUrl;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String badge;
    private String repeatType;
    private LocalDateTime repeatEndAt;
    private Long repeatGroupId;
    private LocalDateTime repeatGroupStartAt;
}