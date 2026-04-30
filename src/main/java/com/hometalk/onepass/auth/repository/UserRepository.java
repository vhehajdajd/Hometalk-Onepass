package com.hometalk.onepass.auth.repository;

import com.hometalk.onepass.auth.entity.User; // 임포트 확인
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
