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

    @Builder
    public LocalAccount(User user, String loginId, String passwordHash) {
        this.user = user;
        this.loginId = loginId;
        this.passwordHash = passwordHash;

    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }
}