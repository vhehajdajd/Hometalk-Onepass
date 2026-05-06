package com.hometalk.onepass.auth.service;

import com.hometalk.onepass.auth.repository.LocalAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DuplicationCheckService {
    private final LocalAccountRepository localAccountRepository;

    public boolean isIdDuplicated(String loginId) {
        if (loginId == null || loginId.trim().isEmpty()) {
            return false;
        }

        // 존재하면 true, 없으면 false
        return localAccountRepository.existsByLoginId(loginId.trim());
    }

}
