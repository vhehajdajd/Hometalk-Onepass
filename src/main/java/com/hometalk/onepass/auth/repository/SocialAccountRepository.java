package com.hometalk.onepass.auth.repository;

import com.hometalk.onepass.auth.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository <SocialAccount, Long> {
    // 플랫폼 별
    Optional<SocialAccount> findByPlatformAndPlatformId(SocialAccount.Platform platform, String platformId);
}
