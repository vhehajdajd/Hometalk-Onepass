package com.hometalk.onepass.schedule.dto;

import com.hometalk.onepass.notice.entity.Badge;
import com.hometalk.onepass.schedule.entity.RepeatType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleRequestDto {

    private Long noticeId; // 연결할 공지 (선택 사항)

    @NotBlank
    @Size(max = 100)
    private String title;

    private String info;

    private String location;

    @Size(max = 500)
    private String referenceUrl;

    @NotNull
    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private Badge badge; // 독립 일정용 배지 (선택 사항)

    private RepeatType repeatType;
    private LocalDateTime repeatEndAt;
    private Long repeatGroupId;

}

