package com.hometalk.onepass.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SocialSignUpDTO {
    // 소셜 제공 데이터
    private String email;
    private String platform;
    private String nickname;
    private String platformId;


    // HTML 추가 입력 데이터 (User/Household용)
    private String name;
    private String phoneNumber;


    // 주소 정보 (Household용)
    private String postNum;
    private String buildingName;
    private String dong;
    private String ho;
}
