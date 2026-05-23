package com.hometalk.onepass.reservation.dto;

// import com.hometalk.onepass.reservation.entity.ReservationTime; <- start,end 한 번에 정의한 것 편하지만 꼬일 수 있음
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class ReservationRequestDto {
    private Long facilityId;

    // [수정 예정] private String memberId;
    private Long userId; // TODO: 팀 공통 User 엔티티의 ID(PK)를 받도록 변경

    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
