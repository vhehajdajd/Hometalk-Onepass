package com.hometalk.onepass.auth.entity;

import com.hometalk.onepass.common.entity.BaseSoftDeleteEntity;
import com.hometalk.onepass.community.entity.Comment;
import com.hometalk.onepass.community.entity.Post;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id")
    private Household household;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "nickname", length = 30)
    private String nickname;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role = UserRole.MEMBER;

    // 연관관계
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private LocalAccount localAccount;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SocialAccount> socialAccounts = new ArrayList<>();

    // community
    @OneToMany(mappedBy = "writer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Post> posts = new ArrayList<>();

    @OneToMany(mappedBy = "writer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

    @Builder
    public User(String name, String nickname, String email,
                String phoneNumber, UserStatus status, UserRole role,
                Household household) {
        this.name = name;
        this.nickname = nickname;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.status = status != null ? status : UserStatus.PENDING;
        this.role = role != null ? role : UserRole.MEMBER;
        this.household = household;
    }

    // 세대 배정 (관리자 승인 시 호출)
    public void assignHousehold(Household household) {
        this.household = household;
    }

    public void removeHousehold() {
        this.household = null;
    }

    public void approve() {
        this.status = UserStatus.APPROVED;
    }

    public void reject() {
        this.status = UserStatus.REJECTED;
    }

    public void withdraw() {
        this.status = UserStatus.REJECTED;
        softDelete();
    }

    // Enum
    public enum UserStatus {
        PENDING, APPROVED, REJECTED
    }

    public enum UserRole {
        ADMIN, RESIDENT, STAFF, MEMBER
    }
}
