package com.hometalk.onepass.facility.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacilityRequestDto {
    private String name;        // 시설 명칭
    private String location;    // 단지 내 상세 위치
    private String iconType;    // UI 아이콘 타입 (TENNIS, BOOK 등)
    private String imagePath;   // 서버 저장용 이미지 경로

    private String openTime;    // 운영 시작 ("09:00")
    private String closeTime;   // 운영 종료 ("22:00")

    @Min(1)
    private int maxCapacity;    // 시설별 최대 인원
    @NotNull @Min(1) @Max(24)
    private Integer maxReservationTime;
}