package com.hometalk.onepass.auth.config;

import com.hometalk.onepass.auth.entity.Household;
import com.hometalk.onepass.auth.entity.LocalAccount;
import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.auth.repository.HouseholdRepository;
import com.hometalk.onepass.auth.repository.LocalAccountRepository;
import com.hometalk.onepass.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final HouseholdRepository householdRepository;
    private final UserRepository userRepository;
    private final LocalAccountRepository localAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (localAccountRepository.count() == 0) {
            log.info("기존 서비스 로직 방식으로 더미 데이터 생성을 시작합니다...");

            // 1. 관리자 계정 생성
            createDummySet("test1", "1234", "홍길동", "길동", "gildong@test.com", "010-0000-0000",
                    "123456", "홈톡아파트", "101동", "101호",
                    User.UserRole.ADMIN, User.UserStatus.APPROVED);

            // 2. 거주자 계정 생성
            createDummySet("test2", "1234", "김철수", "철수", "chulsu@test.com", "010-1111-1111",
                    "345678", "홈톡아파트", "102동", "505호",
                    User.UserRole.RESIDENT, User.UserStatus.APPROVED);

            // 3. 승인 대기 멤버 계정 생성
            createDummySet("test3", "1234", "김영희", "영희", "younghee@test.com", "010-2222-2222",
                    "112233", "홈톡아파트", "103동", "909호",
                    User.UserRole.MEMBER, User.UserStatus.PENDING);

            // 관리자 계정 v2
            createDummySet("admin", "1234", "관리자", "어드민", "admin@hometop.com", "010-1111-2222",
                    "112233", "포레스트 리움", "0", "0",
                    User.UserRole.ADMIN, User.UserStatus.APPROVED);

            // 거주자 계정 v2
            createDummySet("resident", "1234", "홍길동", "길동", "gildong@hometop.com", "010-2222-3333",
                    "112233", "프레스트 리움", "101동", "1101호",
                    User.UserRole.RESIDENT, User.UserStatus.APPROVED);

            // 스태프 계정 v2
            createDummySet("staff", "1234", "김철수", "철수", "chulsoo@hometop.com", "010-5555-6666",
                    "112233", "포레스트 리움", "0", "0",
                    User.UserRole.MEMBER, User.UserStatus.APPROVED);


            log.info("더미 데이터 저장 완료");
        }
    }

    private void createDummySet(String loginId, String password, String name, String nickname, String email, String phone,
                                String postNum, String building, String dong, String ho,
                                User.UserRole role, User.UserStatus status) {

        // 1. Household (세대) 저장
        Household household = Household.builder()
                .postNum(postNum)
                .buildingName(building)
                .dong(dong)
                .ho(ho)
                .build();
        Household savedHousehold = householdRepository.save(household);

        // 2. User (부모) 저장
        User user = User.builder()
                .name(name)
                .email(email)
                .nickname(nickname)
                .phoneNumber(phone)
                .household(savedHousehold) // 세대 연결
                .role(role)
                .status(status)
                .build();
        User savedUser = userRepository.save(user);

        // 3. LocalAccount (자식) 저장
        LocalAccount localAccount = LocalAccount.builder()
                .user(savedUser) // 유저 연결
                .loginId(loginId)
                .passwordHash(passwordEncoder.encode(password))
                .build();
        localAccountRepository.save(localAccount);
    }
}