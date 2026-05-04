package com.hometalk.onepass.facility.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class FacilityResponseDto {
    private Long id;
    private String name;
    private String location;
    private String iconType;

    private String openTime;
    private String closeTime;
}
