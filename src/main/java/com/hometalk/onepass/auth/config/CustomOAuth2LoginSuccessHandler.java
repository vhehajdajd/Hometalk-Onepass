package com.hometalk.onepass.auth.config;

import com.hometalk.onepass.auth.entity.SocialAccount;
import com.hometalk.onepass.auth.repository.SocialAccountRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomOAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    // 소셜 로그인 성공 직후, 우리 서비스 기준으로 이미 가입된 사용자인지 판별한다.
    private final SocialAccountRepository socialAccountRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        // OAuth2AuthenticationToken에서 현재 로그인한 공급자(kakao, naver)를 식별한다.
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
        SocialAccount.Platform platform = SocialAccount.Platform.valueOf(registrationId.toUpperCase());

        String email = "";

        // 1. 데이터 추출 (카카오 vs 네이버)
        if (platform == SocialAccount.Platform.KAKAO) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttributes().get("kakao_account");
            email = (String) kakaoAccount.get("email");
        }
        // 네이버 로직 추가
        else if (platform == SocialAccount.Platform.NAVER) {
            // 네이버는 'response' 안에 실제 정보가 들어있음
            Map<String, Object> responseMap = (Map<String, Object>) oAuth2User.getAttributes().get("response");
            email = (String) responseMap.get("email");
        }

        // 현재 프로젝트는 소셜 계정을 email + platform 조합으로 저장하고 있으므로
        // 로그인 성공 시에도 같은 규칙으로 조회 키를 만들어야 한다.
        String combinedPlatformId = email + "_" + registrationId.toUpperCase();
        log.info("조회하려는 Platform: {}, PlatformId: {}", platform, combinedPlatformId);

        // SocialAccount 존재 여부에 따라 신규 가입 / 기존 사용자 진입으로 분기한다.
        Optional<SocialAccount> socialAccount = socialAccountRepository.findByPlatformAndPlatformId(platform, combinedPlatformId)
                .filter(account -> !account.getUser().isDeleted());
        log.info("DB 존재 여부: {}", socialAccount.isPresent());

        if (socialAccount.isEmpty()) {
            // 신규 소셜 사용자는 추가 회원 정보를 받아야 하므로 별도 가입 화면으로 보낸다.
            String redirectUrl = UriComponentsBuilder.fromUriString(getBaseUrl(request))
                    .path(request.getContextPath())
                    .path("/auth/register/social")
                    .queryParam("email", email)
                    .queryParam("platform", platform)
                    .queryParam("platformId", combinedPlatformId)
                    .build()
                    .toUriString();
            log.info("신규 유저 -> 가입 페이지로 리다이렉트: {}", redirectUrl);
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
            return; // 더 이상 아래 코드가 실행되지 않도록 종료
        } else {
            // 이미 가입된 소셜 계정이면 메인 화면으로 바로 이동시킨다.
            log.info("기존 유저 -> 메인 페이지로 이동");
            String redirectUrl = UriComponentsBuilder.fromUriString(getBaseUrl(request))
                    .path(request.getContextPath())
                    .path("/index")
                    .build()
                    .toUriString();
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
            return;
        }
    }

    private String getBaseUrl(HttpServletRequest request) {
        // 절대 경로 리다이렉트가 필요한 OAuth2 흐름에서 현재 접속 호스트를 기준 URL 로 사용한다.
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
    }
}
