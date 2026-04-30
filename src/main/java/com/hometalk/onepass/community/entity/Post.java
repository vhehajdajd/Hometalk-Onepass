package com.hometalk.onepass.community.entity;

import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.common.entity.BaseSoftDeleteEntity;
import com.hometalk.onepass.community.dto.request.PostRequestDTO;
import com.hometalk.onepass.community.enums.MarketStatus;
import com.hometalk.onepass.community.enums.PostStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)     // Builder 쓸 땐 필수
@Builder
@Table(name = "posts", indexes = {
        @Index(name = "idx_post_user",columnList = "user_id, created_at DESC"),
        @Index(name = "idx_post_category", columnList = "category_id, created_at DESC"),
})
@SQLDelete(sql = "UPDATE posts SET deleted_at = CURRENT_TIMESTAMP, status = 'DELETED' WHERE id = ?")        // delete() 호출 시 실행될 SQL 문
@SQLRestriction("deleted_at IS NULL")
public class Post extends BaseSoftDeleteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Lob @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Builder.Default
    private boolean pinned = false;

    @Column(columnDefinition = "integer default 0", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @Column(columnDefinition = "integer default 0", nullable = false)
    @Builder.Default
    private Integer commentCount = 0;

    // Post가 1인 관계
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PostTag> postTags = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PostImage> postImages = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

    // FK (Post가 N인 관계)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User writer;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", referencedColumnName = "id", nullable = false)
    private Category category;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", referencedColumnName = "id", nullable = false)
    private Board board;

    @Builder.Default
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PostStatus postStatus = PostStatus.ACTIVE;

    @Builder.Default
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MarketStatus marketStatus = MarketStatus.SHARED;

    // 변경 method
    public void update(PostRequestDTO dto) {
        this.title = dto.getTitle();
        this.content = dto.getContent();
    }

    public void togglePinned() {
        this.pinned = !this.pinned;
    }

    public void addPostTag(PostTag postTag) {
        this.postTags.add(postTag);
    }

    @Override
    public void softDelete() {
        super.softDelete();     // 부모 deletedAt 설정 실행
        this.postStatus = PostStatus.DELETED;
    }

    // 조회수
    public void addViewCount() {
        this.viewCount++;
    }

    // 상태 변경 method
    public void updateMarketStatus(MarketStatus newStatus) {
        this.marketStatus = newStatus;
    }
    public void updateStatus(PostStatus status) {
        this.postStatus = status;
    }
}
