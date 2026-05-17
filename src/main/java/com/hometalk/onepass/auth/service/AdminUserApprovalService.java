package com.hometalk.onepass.auth.service;

import com.hometalk.onepass.auth.dto.AdminUserApprovalResponseDTO;
import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserApprovalService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AdminUserApprovalResponseDTO> getPendingUsers() {
        return userRepository.findByStatusAndDeletedAtIsNullOrderByIdDesc(User.UserStatus.PENDING)
                .stream()
                .map(AdminUserApprovalResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<AdminUserApprovalResponseDTO> getPendingUsers(Pageable pageable) {
        return userRepository.findByStatusAndDeletedAtIsNull(User.UserStatus.PENDING, pageable)
                .map(AdminUserApprovalResponseDTO::from);
    }

    @Transactional
    public void approve(Long userId, User.UserRole role) {
        User user = findPendingUser(userId);
        user.approveAs(role);
    }

    @Transactional
    public void reject(Long userId) {
        User user = findPendingUser(userId);
        user.reject();
    }

    private User findPendingUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        if (user.isDeleted()) {
            throw new IllegalStateException("탈퇴한 사용자는 처리할 수 없습니다.");
        }

        if (user.getStatus() != User.UserStatus.PENDING) {
            throw new IllegalStateException("승인 대기 상태의 사용자만 처리할 수 있습니다.");
        }

        return user;
    }
}
