package com.hometalk.onepass.auth.service;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.auth.entity.LocalAccount;
import com.hometalk.onepass.auth.entity.SocialAccount;
import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.auth.repository.LocalAccountRepository;
import com.hometalk.onepass.auth.repository.SocialAccountRepository;
import com.hometalk.onepass.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private static final String LOCAL_PREFIX = "LOCAL:";
    private static final String SOCIAL_PREFIX = "SOCIAL:";

    private final LocalAccountRepository localAccountRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final UserRepository userRepository;

    // Spring Security가 로컬 로그인 또는 remember-me 복구 시 호출하는 진입점이다.
    // 일반 로그인은 loginId가 그대로 들어오고,
    // 자동 로그인 복구는 저장된 username 규칙(LOCAL:, SOCIAL:, userId)에 따라 분기한다.
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username != null && username.startsWith(LOCAL_PREFIX)) {
            return loadLocalUser(username.substring(LOCAL_PREFIX.length()));
        }

        if (username != null && username.startsWith(SOCIAL_PREFIX)) {
            return loadSocialUser(username);
        }

        if (username != null && username.matches("\\d+")) {
            return loadUserById(Long.parseLong(username));
        }

        return loadLocalUser(username);
    }

    // 일반 아이디/비밀번호 로그인 사용자를 LocalAccount.loginId로 조회한다.
    // 조회된 LocalAccount의 User 정보를 principal에 담고, 비밀번호 검증용 passwordHash도 함께 넘긴다.
    private UserDetails loadLocalUser(String loginId) {
        LocalAccount account = localAccountRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("아이디를 찾을 수 없습니다: " + loginId));

        if (account.getUser().isDeleted()) {
            throw new UsernameNotFoundException("탈퇴한 계정입니다: " + loginId);
        }

        // username = loginId로 설정
        return toCustomUserDetails(account.getUser(), account.getLoginId(), account.getPasswordHash());
    }

    // 소셜 remember-me 자동 로그인 복구용 조회 로직이다.
    // username은 SOCIAL:{플랫폼}:{platformId} 형식으로 저장되며,
    // 이 값을 다시 SocialAccount 조회 키로 사용해 서비스 User를 찾는다.
    private UserDetails loadSocialUser(String username) {
        String[] parts = username.split(":", 3);
        if (parts.length != 3) {
            throw new UsernameNotFoundException("소셜 자동로그인 식별자가 올바르지 않습니다: " + username);
        }

        SocialAccount.Platform platform = SocialAccount.Platform.valueOf(parts[1]);
        String platformId = parts[2];
        SocialAccount account = socialAccountRepository.findByPlatformAndPlatformId(platform, platformId)
                .orElseThrow(() -> new UsernameNotFoundException("소셜 계정을 찾을 수 없습니다: " + username));

        if (account.getUser().isDeleted()) {
            throw new UsernameNotFoundException("탈퇴한 계정입니다: " + username);
        }

        return toCustomUserDetails(account.getUser(), username, null);
    }

    // userId만 username으로 저장된 인증 정보를 복구할 때 사용하는 보조 경로다.
    // 현재 User 테이블 기준으로 바로 조회하고, 비밀번호가 필요 없는 인증이므로 password는 null로 둔다.
    private UserDetails loadUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        if (user.isDeleted()) {
            throw new UsernameNotFoundException("탈퇴한 계정입니다: " + userId);
        }

        return toCustomUserDetails(user, String.valueOf(userId), null);
    }

    // DB에서 조회한 User와 로그인 식별자를 Spring Security principal 객체로 변환한다.
    // 컨트롤러의 @AuthenticationPrincipal CustomUserDetails 파라미터에는 이 객체가 주입된다.
    private CustomUserDetails toCustomUserDetails(User user, String loginId, String password) {
        Long householdId = user.getHousehold() != null
                ? user.getHousehold().getId() : null;
        String postNum = user.getHousehold() != null
                ? user.getHousehold().getPostNum() : null;

        return new CustomUserDetails(
                user.getId(),
                householdId,
                postNum,
                user.getName(),
                user.getRole(),
                user.getStatus(),
                user.isApprovalNoticeShown(),
                loginId,
                password,
                user.getNickname()
        );
    }
}
