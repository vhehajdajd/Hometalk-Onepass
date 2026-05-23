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
}