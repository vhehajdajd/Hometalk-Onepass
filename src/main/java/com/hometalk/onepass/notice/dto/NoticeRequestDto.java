package com.hometalk.onepass.notice.dto;

import com.hometalk.onepass.notice.entity.Badge;
import com.hometalk.onepass.notice.entity.NoticeStatus;
import com.hometalk.onepass.schedule.entity.RepeatType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class NoticeRequestDto {

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    @NotNull
    private Boolean isPinned;

    @NotNull
    private Badge badge;

    private NoticeStatus status;

    // 일정 관련 (선택사항)
    private String scheduleName;
    private LocalDateTime scheduleStartAt;
    private LocalDateTime scheduleEndAt;
    private String scheduleInfo;
    private String scheduleLocation;
    private String scheduleReferenceUrl;

    // 반복 일정 추가
    private RepeatType scheduleRepeatType;
    private LocalDateTime scheduleRepeatEndAt;
}