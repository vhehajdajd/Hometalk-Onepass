package com.hometalk.onepass.complaint.dto;

import com.hometalk.onepass.auth.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
    private Long id;
    private String name;
    private String nickname;
    private String email;
    private String phoneNumber;
    private String role;     // UserRole Enum
    private String status;   // UserStatus Enum


    public static UserResponseDto from(User user) {
        if (user == null) return null;

        return UserResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .build();
    }
}