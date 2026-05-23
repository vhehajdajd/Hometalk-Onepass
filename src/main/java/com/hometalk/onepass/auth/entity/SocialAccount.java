package com.hometalk.onepass.auth.entity;
import com.hometalk.onepass.common.entity.BaseTimeEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "social_account",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_social_account_user_platform",
                columnNames = {"user_id", "platform"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20)
    private Platform platform;

    @NotNull
    @Column(name = "platform_id", nullable = false, length = 100)
    private String platformId;

    @Column(name = "social_access_token", columnDefinition = "TEXT")
    private String socialAccessToken;

    @Column(name = "social_refresh_token", columnDefinition = "TEXT")
    private String socialRefreshToken;

    @Column(name = "social_token_expires_at")
    private LocalDateTime socialTokenExpiresAt;

    // JSON 원본 저장 - MySQL JSON 타입 매핑
    @Column(name = "raw_token_data", columnDefinition = "JSON")
    private String rawTokenData;

    @Builder
    public SocialAccount(User user, Platform platform, String platformId,
                         String socialAccessToken, String socialRefreshToken,
                         LocalDateTime socialTokenExpiresAt, String rawTokenData) {
        this.user = user;
        this.platform = platform;
        this.platformId = platformId;
        this.socialAccessToken = socialAccessToken;
        this.socialRefreshToken = socialRefreshToken;
        this.socialTokenExpiresAt = socialTokenExpiresAt;
        this.rawTokenData = rawTokenData;
    }

    public void updateTokens(String accessToken, String refreshToken,
                             LocalDateTime expiresAt, String rawData) {
        this.socialAccessToken = accessToken;
        this.socialRefreshToken = refreshToken;
        this.socialTokenExpiresAt = expiresAt;
        this.rawTokenData = rawData;
    }

    // Enum
    public enum Platform {
        KAKAO, NAVER, GOOGLE
    }
}