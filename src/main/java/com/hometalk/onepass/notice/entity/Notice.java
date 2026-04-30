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


    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "VARCHAR(20)")
    private NoticeStatus status;

    // 공지 생성용
    public void create(String title, String content, Boolean isPinned, Badge badge, User user, NoticeStatus status) {
        this.title = title;
        this.content = content;
        this.isPinned = isPinned;
        this.badge = badge;
        this.user = user;
        this.status = status;
    }

    // 공지 수정용
    public void update(String title, String content, Boolean isPinned, Badge badge, NoticeStatus status) {
        this.title = title;
        this.content = content;
        this.isPinned = isPinned;
        this.badge = badge;
        this.status = status;
    }
    // 조회수 증가
    public void increaseViewCount() {
        this.viewCount++;
    }


    public void publish() {
        this.status = NoticeStatus.PUBLISHED;
    }
}