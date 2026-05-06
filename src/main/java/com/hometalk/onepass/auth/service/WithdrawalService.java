package com.hometalk.onepass.auth.service;

import com.hometalk.onepass.auth.entity.Household;
import com.hometalk.onepass.auth.entity.LocalAccount;
import com.hometalk.onepass.auth.entity.SocialAccount;
import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.auth.repository.HouseholdRepository;
import com.hometalk.onepass.auth.repository.LocalAccountRepository;
import com.hometalk.onepass.auth.repository.SocialAccountRepository;
import com.hometalk.onepass.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class WithdrawalService {

    // 탈퇴 시 계정 연결, 세대 정보, 사용자 상태를 함께 정리해야 해서
    // 관련 저장소를 한 서비스에서 관리한다.
    private final UserRepository userRepository;
    private final HouseholdRepository householdRepository;
    private final LocalAccountRepository localAccountRepository;
    private final SocialAccountRepository socialAccountRepository;

    public void withdraw(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("로그인한 사용자만 탈퇴할 수 있습니다.");
        }

        User user = resolveUser(authentication);

        // 로컬 계정은 loginId 이력 보존을 위해 남겨두고,
        // 소셜 계정은 제거해서 같은 공급자 계정으로 재가입할 수 있게 한다.
        // user 자체는 소프트 삭제 성격으로 남기고, 재연결 충돌 가능성이 있는 소셜 계정만 제거한다.
        if (!user.getSocialAccounts().isEmpty()) {
            socialAccountRepository.deleteAll(new ArrayList<>(user.getSocialAccounts()));
        }

        Household household = user.getHousehold();
        user.removeHousehold();
        user.withdraw();

        if (household != null) {
            householdRepository.delete(household);
        }
    }

    private User resolveUser(Authentication authentication) {
        // Authentication 구현체에 따라 사용자 식별 방식이 다르므로 먼저 분기한다.
        if (authentication instanceof OAuth2AuthenticationToken oauthToken
                && authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
            return findSocialUser(oauthToken, oAuth2User);
        }

        return findLocalUser(authentication.getName());
    }

    private User findLocalUser(String loginId) {
        LocalAccount account = localAccountRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalStateException("탈퇴할 로컬 계정을 찾을 수 없습니다."));

        if (account.getUser().isDeleted()) {
            throw new IllegalStateException("이미 탈퇴한 회원입니다.");
        }

        return account.getUser();
    }

    private User findSocialUser(OAuth2AuthenticationToken oauthToken, OAuth2User oAuth2User) {
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        SocialAccount.Platform platform = SocialAccount.Platform.valueOf(registrationId.toUpperCase());
        String email = extractEmail(platform, oAuth2User.getAttributes());

        if (email == null || email.isBlank()) {
            throw new IllegalStateException("소셜 계정 이메일 정보를 찾을 수 없습니다.");
        }

        String combinedPlatformId = email + "_" + registrationId.toUpperCase();
        SocialAccount socialAccount = socialAccountRepository.findByPlatformAndPlatformId(platform, combinedPlatformId)
                .orElseThrow(() -> new IllegalStateException("탈퇴할 소셜 계정을 찾을 수 없습니다."));

        if (socialAccount.getUser().isDeleted()) {
            throw new IllegalStateException("이미 탈퇴한 회원입니다.");
        }

        return socialAccount.getUser();
    }


    private String extractEmail(SocialAccount.Platform platform, Map<String, Object> attributes) {
        // 마이페이지 조회와 동일한 규칙으로 공급자별 email 위치를 읽는다.
        if (platform == SocialAccount.Platform.KAKAO) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            return kakaoAccount == null ? null : (String) kakaoAccount.get("email");
        }

        if (platform == SocialAccount.Platform.NAVER) {
            Map<String, Object> response = (Map<String, Object>) attributes.get("response");
            return response == null ? null : (String) response.get("email");
        }

        return (String) attributes.get("email");
    }
}
