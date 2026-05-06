package com.hometalk.onepass.auth.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CustomAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    // Spring Security 기본 OAuth2 인가 요청 생성기를 감싸서
    // 특정 공급자에만 추가 파라미터를 주입하기 위해 사용한다.
    private final OAuth2AuthorizationRequestResolver defaultResolver;


    // 인증 요청 시 반환
    public CustomAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {  // 인증 정보 관리 레포지터리
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository, "/oauth2/authorization");
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        // "/oauth2/authorization/{registrationId}" 같은 일반 진입 경로는
        // 기본 resolver 로 표준 OAuth2 요청을 만든 뒤 후처리만 추가함
        OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request);
        return customizeAuthorizationRequest(authorizationRequest);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        // registrationId 가 이미 분리되어 넘어오는 경로도 있으므로
        // 두 오버로드 모두 같은 후처리 로직을 타도록 맞춘다.
        OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request, clientRegistrationId);
        return customizeAuthorizationRequest(authorizationRequest);
    }

    private OAuth2AuthorizationRequest customizeAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest) {
        if (authorizationRequest == null) {
            // 현재 요청이 OAuth2 인가 요청이 아니면 수정 없이 그대로 빠져나간다.
            return null;
        }

        String registrationId = authorizationRequest.getAttribute("registration_id");
        if (!"kakao".equals(registrationId)) {
            // 네이버를 포함한 다른 공급자는 기본 요청 그대로 사용한다.
            return authorizationRequest;
        }

        // 카카오는 브라우저에 기존 카카오 로그인 세션이 남아 있으면
        // 계정 선택 없이 바로 로그인될 수 있으므로 prompt=login을 강제한다.
        // 다른 카카오 계정으로 재로그인할 때도 항상 계정 선택 화면을 보장하려는 의도다.
        // LinkedHashMap 으로 복사하는 이유는 기존 추가 파라미터를 보존한 채
        // 필요한 항목만 덮어쓰기 위해서다.
        Map<String, Object> additionalParameters = new LinkedHashMap<>(authorizationRequest.getAdditionalParameters());
        additionalParameters.put("prompt", "login");

        // 기존 authorizationRequest 를 기반으로 새 요청을 다시 만들어
        // scope, redirect_uri 같은 표준 값은 유지하고 추가 파라미터만 교체한다.
        return OAuth2AuthorizationRequest.from(authorizationRequest)
                .additionalParameters(additionalParameters)
                .build();
    }
}
