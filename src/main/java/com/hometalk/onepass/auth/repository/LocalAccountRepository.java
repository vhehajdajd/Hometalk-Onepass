package com.hometalk.onepass.auth.repository;

import com.hometalk.onepass.auth.entity.LocalAccount;
import com.hometalk.onepass.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocalAccountRepository extends JpaRepository<LocalAccount, Long> {
    Long user(User user);

    Optional<LocalAccount> findByLoginId(String loginId);
}
