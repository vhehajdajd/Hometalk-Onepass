package com.hometalk.onepass.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyPageResponseDTO {

    private final String authType;
    private final String socialPlatform;
    private final String name;
    private final String nickname;
    private final String email;
    private final String phoneNumber;
    private final String buildingName;
    private final String dong;
    private final String ho;
    private final String postNum;
}
