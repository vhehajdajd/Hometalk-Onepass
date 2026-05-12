package com.hometalk.onepass.facility.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    @NonNull @Min(1) @Max(24)
    private Integer maxReservationTime;
}