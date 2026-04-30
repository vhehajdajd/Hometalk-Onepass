package com.hometalk.onepass.notice.entity;

import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class Notice extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private Boolean isPinned;

    private int viewCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "badge", columnDefinition = "VARCHAR(20)")
    private Badge badge;

    // 공지 생성용
    public void create(String title, String content, Boolean isPinned, Badge badge, User user) {
        this.title = title;
        this.content = content;
        this.isPinned = isPinned;
        this.badge = badge;
        this.user = user;
    }

    // 공지 수정용
    public void update(String title, String content, Boolean isPinned, Badge badge) {
        this.title = title;
        this.content = content;
        this.isPinned = isPinned;
        this.badge = badge;
    }

    // 조회수 증가
    public void increaseViewCount() {
        this.viewCount++;
    }
}