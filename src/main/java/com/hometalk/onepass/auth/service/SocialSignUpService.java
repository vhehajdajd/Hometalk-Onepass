package com.hometalk.onepass.auth.service;

import com.hometalk.onepass.auth.dto.SocialSignUpDTO;
import com.hometalk.onepass.auth.entity.Household;
import com.hometalk.onepass.auth.entity.SocialAccount;
import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.auth.repository.HouseholdRepository;
import com.hometalk.onepass.auth.repository.SocialAccountRepository;
import com.hometalk.onepass.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class SocialSignUpService {

    private final HouseholdRepository householdRepository;
    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;

    @Transactional
    public User socialSignUp(SocialSignUpDTO dto) {

        // 1. Household (세대 정보) 생성 및 저장
        Household household = Household.builder()
                .postNum(dto.getPostNum())
                .buildingName(dto.getBuildingName())
                .dong(dto.getDong())
                .ho(dto.getHo())
                .build();
        Household savedHousehold = householdRepository.save(household);

        // 2. User (부모) 생성 및 저장
        User user = User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .nickname(dto.getNickname())
                .phoneNumber(dto.getPhoneNumber())
                .household(savedHousehold) // 세대 연결
                .status(User.UserStatus.PENDING) // 기본값 설정
                .role(User.UserRole.MEMBER)      // 기본값 설정
                .build();
        User savedUser = userRepository.save(user);

        // 3. platformId 가공 (email + platform)
        // 예: test@kakao.com_KAKAO
        String combinedPlatformId = dto.getEmail() + "_" + dto.getPlatform().toUpperCase();

        // 4. SocialAccount (자식) 생성 및 저장
        SocialAccount socialAccount = SocialAccount.builder()
                .user(savedUser)
                .platform(SocialAccount.Platform.valueOf(dto.getPlatform().toUpperCase()))
                .platformId(combinedPlatformId)
                .build();

        socialAccountRepository.save(socialAccount);

        return savedUser;
    }

    @Transactional(readOnly = true)
    public SocialSignUpDTO getRejectedSocialSignUpForm(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (user.getStatus() != User.UserStatus.REJECTED) {
            throw new IllegalArgumentException("거절된 회원만 정보를 수정할 수 있습니다.");
        }

        SocialAccount socialAccount = socialAccountRepository.findFirstByUser_Id(userId)
                .orElseThrow(() -> new IllegalArgumentException("소셜 계정 정보를 찾을 수 없습니다."));

        SocialSignUpDTO dto = new SocialSignUpDTO();
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setNickname(user.getNickname());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setPlatform(socialAccount.getPlatform().name());
        dto.setPlatformId(socialAccount.getPlatformId());

        Household household = user.getHousehold();
        if (household != null) {
            dto.setPostNum(household.getPostNum());
            dto.setBuildingName(household.getBuildingName());
            dto.setDong(household.getDong());
            dto.setHo(household.getHo());
        }

        return dto;
    }

    @Transactional
    public User updateRejectedSocialSignUp(Long userId, SocialSignUpDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (user.getStatus() != User.UserStatus.REJECTED) {
            throw new IllegalArgumentException("거절된 회원만 정보를 수정할 수 있습니다.");
        }

        socialAccountRepository.findFirstByUser_Id(userId)
                .orElseThrow(() -> new IllegalArgumentException("소셜 계정 정보를 찾을 수 없습니다."));

        Household household = Household.builder()
                .postNum(dto.getPostNum())
                .buildingName(dto.getBuildingName())
                .dong(dto.getDong())
                .ho(dto.getHo())
                .build();
        Household savedHousehold = householdRepository.save(household);

        user.updateProfile(dto.getName(), dto.getNickname(), dto.getEmail(), dto.getPhoneNumber());
        user.assignHousehold(savedHousehold);
        user.resubmitForApproval();

        return user;
    }
}
