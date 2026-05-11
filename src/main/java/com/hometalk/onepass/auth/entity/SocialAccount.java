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

    @Builder
    public SocialAccount(User user, Platform platform, String platformId) {
        this.user = user;
        this.platform = platform;
        this.platformId = platformId;
    }


    // Enum
    public enum Platform {
        KAKAO, NAVER, GOOGLE
    }
}