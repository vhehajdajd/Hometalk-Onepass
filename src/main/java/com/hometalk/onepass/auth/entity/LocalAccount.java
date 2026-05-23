package com.hometalk.onepass.auth.entity;

import com.hometalk.onepass.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Valid
@Entity
@Table(name = "local_account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LocalAccount extends BaseTimeEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    // user_id가 PK이자 FK이므로 @MapsId 사용
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @NotNull
    @Column(name = "login_id", nullable = false, unique = true, length = 100)
    private String loginId;

    @NotNull
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @NotNull
    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    @Column(name = "email_verify_token", length = 255)
    private String emailVerifyToken;

    @Builder
    public LocalAccount(User user, String loginId, String passwordHash,
                        String emailVerifyToken) {
        this.user = user;
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.emailVerified = false;
        this.emailVerifyToken = emailVerifyToken;
    }

    public void verifyEmail() {
        this.emailVerified = true;
        this.emailVerifyToken = null;
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }
}