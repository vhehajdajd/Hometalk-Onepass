package com.hometalk.onepass.auth.config;

import com.hometalk.onepass.auth.entity.User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

// OAuth2 로그인 성공 후 SecurityContext에 저장되는 principal 객체.
// 일반 로그인의 CustomUserDetails와 같은 사용자 필드를 제공하면서,
// OAuth2User 인터페이스도 구현해서 소셜 로그인 흐름과 호환되도록 한다.
public class CustomOAuth2User extends CustomUserDetails implements OAuth2User {

    // 공급자별 원본 응답 데이터.
    // Kakao/Naver/Google은 email, id 등의 위치가 달라서 후속 로직에서 원본 attributes가 필요할 수 있다.
    private final Map<String, Object> attributes;

    public CustomOAuth2User(Long userId, Long householdId, String postNum,
                            String name, String nickname, User.UserRole role,
                            User.UserStatus status, boolean approvalNoticeShown,
                            String loginId, String password,
                            Map<String, Object> attributes) {
        super(userId, householdId, postNum, name, role, status, approvalNoticeShown, loginId, password, nickname);
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        // OAuth2User의 대표 식별자.
        // remember-me 복구와 같은 식별자를 쓰기 위해 getUsername() 값을 그대로 사용한다.
        return getUsername();
    }
}
