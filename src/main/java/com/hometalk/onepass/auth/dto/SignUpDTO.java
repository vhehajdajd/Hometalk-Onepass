package com.hometalk.onepass.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter @Setter @ToString
public class SignUpDTO {
    // User 엔티티 (기본 정보) 매핑용
    @NotNull
    private String name;
    @NotNull
    private String email;
    private String nickname;
    private String phoneNumber;

    // LocalAccount 엔티티 (로그인 계정) 매핑용
    @NotNull
    private String loginId;  // HTML의 name="loginId"와 일치
    @NotNull
    private String password; // HTML의 name="password"와 일치

    // 주소 관련 엔티티
    private String buildingName;
    private String dong;
    private String ho;
    private String postNum;
}