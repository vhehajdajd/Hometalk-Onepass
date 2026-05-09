package com.hometalk.onepass.auth.repository;

import com.hometalk.onepass.auth.entity.User; // 임포트 확인
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByStatusAndDeletedAtIsNullOrderByIdDesc(User.UserStatus status);

    Page<User> findByStatusAndDeletedAtIsNull(User.UserStatus status, Pageable pageable);
}
