package com.hometalk.onepass.auth.service;

import com.hometalk.onepass.auth.dto.SignUpDTO;
import com.hometalk.onepass.auth.entity.Household;
import com.hometalk.onepass.auth.entity.LocalAccount;
import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.auth.repository.HouseholdRepository;
import com.hometalk.onepass.auth.repository.LocalAccountRepository;
import com.hometalk.onepass.auth.repository.UserRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Validated
@Service
@RequiredArgsConstructor
public class SignUpService {

    private final HouseholdRepository householdRepository;
    private final UserRepository userRepository;
    private final LocalAccountRepository localAccountRepository;
    private final BCryptPasswordEncoder bcryptPasswordEncoder;

    // 회원 가입 서비스
    @Transactional
    public void signUp(SignUpDTO dto) {
        validateLoginIdAvailable(dto.getLoginId());

        // 1. Household (세대 정보) 생성 및 저장
        // 세대 정보는 여러 유저가 공유할 수 있으나, 가입 시점에 생성하는 로직으로 작성합니다.
        Household household = Household.builder()
                .postNum(dto.getPostNum())
                .buildingName(dto.getBuildingName())
                .dong(dto.getDong())
                .ho(dto.getHo())
                .build();

        Household savedHousehold = householdRepository.save(household); // 2. 세대 먼저 저장

        // 2. User (부모) 생성
        User user = User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .nickname(dto.getNickname())
                .phoneNumber(dto.getPhoneNumber())
                .household(savedHousehold) // 3. 유저에게 세대 정보 연결
                .build();

        User savedUser = userRepository.save(user);

        // 3. LocalAccount (자식) 생성
        LocalAccount localAccount = LocalAccount.builder()
                .user(savedUser)
                .loginId(dto.getLoginId())
                .passwordHash(bcryptPasswordEncoder.encode(dto.getPassword()))
                .build();

        localAccountRepository.save(localAccount);
    }

    @Transactional
    public SignUpDTO getRejectedSignUpForm(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (user.getStatus() != User.UserStatus.REJECTED) {
            throw new IllegalArgumentException("거절된 회원만 정보를 수정할 수 있습니다.");
        }

        LocalAccount localAccount = user.getLocalAccount();
        if (localAccount == null) {
            throw new IllegalArgumentException("소셜 가입 사용자는 이 화면에서 계정 정보를 수정할 수 없습니다.");
        }

        SignUpDTO dto = new SignUpDTO();
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setNickname(user.getNickname());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setLoginId(localAccount.getLoginId());

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
    public User updateRejectedSignUp(Long userId, SignUpDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (user.getStatus() != User.UserStatus.REJECTED) {
            throw new IllegalArgumentException("거절된 회원만 정보를 수정할 수 있습니다.");
        }

        LocalAccount localAccount = user.getLocalAccount();
        if (localAccount == null) {
            throw new IllegalArgumentException("소셜 가입 사용자는 이 화면에서 계정 정보를 수정할 수 없습니다.");
        }

        validateLoginIdAvailableForUser(dto.getLoginId(), userId);

        Household savedHousehold = updateOrCreateHousehold(user.getHousehold(),
                dto.getPostNum(), dto.getBuildingName(), dto.getDong(), dto.getHo());

        user.updateProfile(dto.getName(), dto.getNickname(), dto.getEmail(), dto.getPhoneNumber());
        user.assignHousehold(savedHousehold);
        user.resubmitForApproval();
        localAccount.changeLoginId(dto.getLoginId());

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            localAccount.changePassword(bcryptPasswordEncoder.encode(dto.getPassword()));
        }

        return user;
    }

    private Household updateOrCreateHousehold(
            Household household, String postNum, String buildingName, String dong, String ho) {
        if (household != null) {
            household.updateAddress(postNum, buildingName, dong, ho);
            return household;
        }

        Household newHousehold = Household.builder()
                .postNum(postNum)
                .buildingName(buildingName)
                .dong(dong)
                .ho(ho)
                .build();

        return householdRepository.save(newHousehold);
    }

    public void validateLoginIdAvailable(String loginId) {
        if (loginId == null || loginId.trim().isEmpty()) {
            throw new IllegalArgumentException("아이디를 입력해 주세요.");
        }

        if (localAccountRepository.existsByLoginId(loginId.trim())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
    }

    public void validateLoginIdAvailableForUser(String loginId, Long userId) {
        if (loginId == null || loginId.trim().isEmpty()) {
            throw new IllegalArgumentException("아이디를 입력해 주세요.");
        }

        localAccountRepository.findByLoginId(loginId.trim())
                .filter(account -> !account.getUserId().equals(userId))
                .ifPresent(account -> {
                    throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
                });
    }

    public void validateEmailAvailable(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("이메일을 입력해 주세요.");
        }

        if (userRepository.existsByEmail(email.trim())) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
    }
}
