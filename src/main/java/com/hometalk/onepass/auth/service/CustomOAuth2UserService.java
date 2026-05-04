package com.hometalk.onepass.auth.service;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 공급자(Kakao/Naver)에서 내려준 사용자 원본 정보를 먼저 조회한다.
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 현재는 후처리 없이 원본 OAuth2User를 그대로 반환하고,
        // 실제 가입 여부 판단과 후속 이동은 SuccessHandler에서 담당한다.
        return oAuth2User;
    }
}
