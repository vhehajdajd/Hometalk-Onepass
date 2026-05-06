package com.hometalk.onepass.auth.dto;

import com.hometalk.onepass.auth.entity.Household;
import com.hometalk.onepass.auth.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminUserApprovalResponseDTO {

    private Long userId;
    private String name;
    private String nickname;
    private String email;
    private String phoneNumber;
    private String role;
    private String status;
    private String buildingName;
    private String dong;
    private String ho;
    private String postNum;

    public static AdminUserApprovalResponseDTO from(User user) {
        Household household = user.getHousehold();

        return AdminUserApprovalResponseDTO.builder()
                .userId(user.getId())
                .name(user.getName())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .buildingName(household != null ? household.getBuildingName() : null)
                .dong(household != null ? household.getDong() : null)
                .ho(household != null ? household.getHo() : null)
                .postNum(household != null ? household.getPostNum() : null)
                .build();
    }
}
