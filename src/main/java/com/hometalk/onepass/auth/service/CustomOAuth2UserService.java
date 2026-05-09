package com.hometalk.onepass.auth.service;

import com.hometalk.onepass.auth.config.CustomOAuth2User;
import com.hometalk.onepass.auth.entity.SocialAccount;
import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.auth.repository.SocialAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    // OAuth 공급자 계정과 우리 서비스 User를 연결하는 테이블을 조회한다.
    // 기존 가입 소셜 회원이면 이 정보를 기준으로 CustomOAuth2User를 만든다.
    private final SocialAccountRepository socialAccountRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 공급자(Kakao/Naver/Google)에서 내려준 사용자 원본 정보를 먼저 조회한다.
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // registrationId는 application.yaml에 등록한 provider 식별자(kakao, naver, google)이다.
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        SocialAccount.Platform platform = SocialAccount.Platform.valueOf(registrationId.toUpperCase());
        String email = extractEmail(platform, oAuth2User.getAttributes());

        // 이메일이 없으면 우리 서비스의 SocialAccount 조회 키를 만들 수 없다.
        // 이 경우는 기존 OAuth2User를 그대로 넘겨서 successHandler에서 기존 흐름대로 처리하게 둔다.
        if (email == null || email.isBlank()) {
            return oAuth2User;
        }

        // 소셜 가입 시 저장하는 platformId 규칙과 반드시 같아야 한다.
        String combinedPlatformId = email + "_" + registrationId.toUpperCase();

        return socialAccountRepository.findByPlatformAndPlatformId(platform, combinedPlatformId)
                .filter(account -> !account.getUser().isDeleted())
                // 이미 가입된 소셜 회원은 일반 로그인과 동일하게 CustomUserDetails 계열 principal로 변환한다.
                .map(account -> toCustomOAuth2User(account, platform, combinedPlatformId, oAuth2User.getAttributes()))
                // 신규 소셜 사용자는 아직 우리 서비스 User가 없으므로 가입 분기는 SuccessHandler에 맡긴다.
                .map(OAuth2User.class::cast)
                .orElse(oAuth2User);
    }

    private CustomOAuth2User toCustomOAuth2User(SocialAccount account, SocialAccount.Platform platform,
                                                String platformId, Map<String, Object> attributes) {
        User user = account.getUser();
        // 세대 배정 전 사용자도 로그인할 수 있으므로 household 관련 값은 null-safe 하게 담는다.
        Long householdId = user.getHousehold() != null
                ? user.getHousehold().getId() : null;
        String postNum = user.getHousehold() != null
                ? user.getHousehold().getPostNum() : null;

        String rememberMeUsername = "SOCIAL:" + platform.name() + ":" + platformId;

        return new CustomOAuth2User(
                user.getId(),
                householdId,
                postNum,
                user.getName(),
                user.getNickname(),
                user.getRole(),
                user.getStatus(),
                user.isApprovalNoticeShown(),
                rememberMeUsername,
                null,
                attributes
        );
    }

    @SuppressWarnings("unchecked")
    private String extractEmail(SocialAccount.Platform platform, Map<String, Object> attributes) {
        // OAuth 공급자마다 사용자 정보 JSON 구조가 달라서 platform별로 email 위치를 분기한다.
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
