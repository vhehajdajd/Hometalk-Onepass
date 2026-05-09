package com.hometalk.onepass.auth.service;

import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserApprovalStatusService {

    private final UserRepository userRepository;

    @Transactional
    public void markApprovalNoticeShown(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        if (user.getStatus() == User.UserStatus.APPROVED && !user.isApprovalNoticeShown()) {
            user.markApprovalNoticeShown();
        }
    }
}
