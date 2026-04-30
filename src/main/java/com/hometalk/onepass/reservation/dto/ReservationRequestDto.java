package com.hometalk.onepass.reservation.dto;

// import com.hometalk.onepass.reservation.entity.ReservationTime; <- start,end 한 번에 정의한 것 편하지만 꼬일 수 있음
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class ReservationRequestDto {
    private String facilityName; // facilityId 대신 이름으로 찾기로 하셨다면 String
    private String reservationDate; // "2026-05-09" 형태의 문자열
    private String startTime; // "19:00" 형태의 문자열
    private String endTime;   // "21:00" 형태의 문자열
    private Long userId;
}
