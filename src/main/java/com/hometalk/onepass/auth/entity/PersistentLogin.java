package com.hometalk.onepass.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "persistent_logins")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersistentLogin {

    // Spring Security PersistentTokenBasedRememberMeServices가 사용하는 사용자 식별자.
    // 통합 자동로그인에서는 LOCAL:{loginId}, SOCIAL:{PLATFORM}:{platformId} 형태로 저장할 예정이다.
    @Column(name = "username", nullable = false, length = 64)
    private String username;

    // 브라우저/기기 단위 식별자이며 remember-me 쿠키와 DB row를 연결하는 primary key다.
    @Id
    @Column(name = "series", nullable = false, length = 64)
    private String series;

    // 쿠키 검증용 토큰 값. 자동로그인 성공 시 Spring Security가 새 값으로 회전시킨다.
    @Column(name = "token", nullable = false, length = 64)
    private String token;

    // 마지막 자동로그인 사용 시각. 토큰 만료 판단과 갱신에 사용된다.
    @Column(name = "last_used", nullable = false)
    private LocalDateTime lastUsed;
}
