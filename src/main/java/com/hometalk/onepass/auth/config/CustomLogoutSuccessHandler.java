package com.hometalk.onepass.auth.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    // 카카오 계정 로그아웃 URL 구성 시 사용할 REST API 키
    private final String kakaoClientId;

    public CustomLogoutSuccessHandler(
            @Value("${spring.security.oauth2.client.registration.kakao.client-id}") String kakaoClientId) {
        this.kakaoClientId = kakaoClientId;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
                                Authentication authentication) throws IOException, ServletException {

        // OAuth2 로그인 사용자는 공급자별 로그아웃 후처리를 수행한다.
        // 로컬 로그인은 세션 종료만으로 충분하지만,
        // 소셜 로그인은 공급자 세션까지 정리해야 다음 로그인 흐름이 예측 가능하다.
        if (authentication instanceof OAuth2AuthenticationToken oauth2AuthenticationToken) {
            String registrationId = oauth2AuthenticationToken.getAuthorizedClientRegistrationId();

            if ("kakao".equals(registrationId)) {
                redirectToKakaoLogout(request, response);
                return;
            }

            if ("naver".equals(registrationId)) {
                // 네이버는 프로젝트에서 별도로 구현한 로그아웃 콜백 엔드포인트로 보낸다.
                String naverLogoutUrl = UriComponentsBuilder.fromUriString(getBaseUrl(request))
                        .path(request.getContextPath())
                        .path("/auth/oauth2/naver/logout")
                        .build()
                        .toUriString();
                response.sendRedirect(naverLogoutUrl);
                return;
            }
        }

        response.sendRedirect(request.getContextPath() + "/auth");
    }

    private void redirectToKakaoLogout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 우리 서비스 세션이 종료된 뒤,
        // 카카오 로그인 사용자인 경우 카카오 인증 서버 로그아웃까지 이어서 수행한다.
        // 카카오 로그아웃 후에는 다시 우리 로그인 화면으로 복귀시킨다.
        String logoutRedirectUri = UriComponentsBuilder.fromUriString(getBaseUrl(request))
                .path(request.getContextPath())
                .path("/auth")
                .build()
                .toUriString();

        // 로컬 로그인 사용자는 이 분기를 타지 않고 바로 아래 /auth 리다이렉트로 끝난다.
        String kakaoLogoutUrl = UriComponentsBuilder
                .fromUriString("http://localhost:8090/hometop/auth/oauth2/kakao/logout")
                .queryParam("client_id", kakaoClientId)
                .queryParam("logout_redirect_uri", logoutRedirectUri)
                .build()
                .toUriString();

        response.sendRedirect(kakaoLogoutUrl);
    }

    private String getBaseUrl(HttpServletRequest request) {
        // 배포 환경에서도 현재 요청 기준으로 프로토콜/호스트/포트를 동적으로 맞춘다.
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
    }
}
