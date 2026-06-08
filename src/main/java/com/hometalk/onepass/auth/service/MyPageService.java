package com.hometalk.onepass.auth.service;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.auth.dto.MyPageResponseDTO;
import com.hometalk.onepass.auth.entity.Household;
import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private static final String SOCIAL_PREFIX = "SOCIAL:";

    private final UserRepository userRepository;

    public MyPageResponseDTO getMyPage(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalStateException("로그인한 사용자 정보가 없습니다.");
        }

        User user = userRepository.findById(userDetails.getUserId())
                .filter(foundUser -> !foundUser.isDeleted())
                .orElseThrow(() -> new IllegalStateException("사용자 정보를 찾을 수 없습니다."));

        return toDto(user, resolveAuthType(userDetails), resolveSocialPlatform(userDetails));
    }

    private String resolveAuthType(CustomUserDetails userDetails) {
        return isSocialLogin(userDetails) ? "소셜 로그인" : "일반 로그인";
    }

    private String resolveSocialPlatform(CustomUserDetails userDetails) {
        if (!isSocialLogin(userDetails)) {
            return null;
        }

        String[] parts = userDetails.getUsername().split(":", 3);
        return parts.length >= 2 ? parts[1] : null;
    }

    private boolean isSocialLogin(CustomUserDetails userDetails) {
        return userDetails.getUsername() != null && userDetails.getUsername().startsWith(SOCIAL_PREFIX);
    }

    private MyPageResponseDTO toDto(User user, String authType, String socialPlatform) {
        Household household = user.getHousehold();

        // 세대 정보는 연결 전 사용자도 있을 수 있어서 null-safe 하게 채운다.
        return MyPageResponseDTO.builder()
                .authType(authType)
                .socialPlatform(socialPlatform)
                .role(user.getRole().name())
                .name(user.getName())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .buildingName(household != null ? household.getBuildingName() : null)
                .dong(household != null ? household.getDong() : null)
                .ho(household != null ? household.getHo() : null)
                .postNum(household != null ? household.getPostNum() : null)
                .build();
    }
}
