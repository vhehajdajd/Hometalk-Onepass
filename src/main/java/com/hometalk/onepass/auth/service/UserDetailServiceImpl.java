package com.hometalk.onepass.auth.service;

import com.hometalk.onepass.auth.entity.LocalAccount;
import com.hometalk.onepass.auth.repository.LocalAccountRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDetailServiceImpl implements UserDetailsService {

    private final LocalAccountRepository localAccountRepository;

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        LocalAccount account = localAccountRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("아이디를 찾을 수 없습니다: " + loginId));

        if (account.getUser().isDeleted()) {
            throw new UsernameNotFoundException("탈퇴한 계정입니다: " + loginId);
        }

        // username = loginId로 설정
        return User.builder()
                .username(account.getLoginId())
                .password(account.getPasswordHash())
                .roles(account.getUser().getRole().name())
                .build();
    }
}
