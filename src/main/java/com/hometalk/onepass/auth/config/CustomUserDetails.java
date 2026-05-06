package com.hometalk.onepass.auth.config;
import com.hometalk.onepass.auth.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final Long householdId;
    private final String postNum;       // 관리자의 경우에도 post_num이 필요하여 추가
    private final String name;
    private final User.UserRole role;
    private final String loginId;
    private final String password;

    public CustomUserDetails(Long userId, Long householdId, String postNum,
    String name, User.UserRole role,
    String loginId, String password) {
        this.userId      = userId;
        this.householdId = householdId;
        this.postNum     = postNum;     // 관리자의 경우에도 post_num이 필요하여 추가
        this.name        = name;
        this.role        = role;
        this.loginId     = loginId;
        this.password    = password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public String getPassword()             { return password; }
    @Override public String getUsername()             { return loginId; }
    @Override public boolean isAccountNonExpired()    { return true; }
    @Override public boolean isAccountNonLocked()     { return true; }
    @Override public boolean isCredentialsNonExpired(){ return true; }
    @Override public boolean isEnabled()              { return true; }
}