package com.hometalk.onepass.inquiry.entity;

import com.hometalk.onepass.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "kjh_complaint")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Setter 대신 비즈니스 의미가 담긴 메서드를 선언하는 게 좋지만, 
    // 서비스에서 유저를 주입하기 위해 열어두겠습니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public void setUser(User user) {
        this.user = user;
    }

    private String category;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private boolean isSecret;
    private int viewCount;
    private String status;
    private String answer;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.viewCount = 0;
        if (this.status == null) {
            this.status = "접수완료";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 관리자 답변 등록 로직 (이미 잘 짜셨어요!)
    public void addResponse(String response) {
        this.answer = response;
        this.status = "처리완료";
    }

    // 파일 업로드 연관관계 설정
    @Builder.Default // 빌더 사용 시 초기화된 리스트가 무시되지 않도록 설정
    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ComplaintAttachment> attachments = new ArrayList<>();

    // 조회수 증가 메서드 (상세보기 할 때 사용)
    public void incrementViewCount() {
        this.viewCount++;
    }
}