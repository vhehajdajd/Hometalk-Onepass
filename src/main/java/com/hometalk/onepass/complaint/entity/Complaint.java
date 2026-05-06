package com.hometalk.onepass.complaint.entity;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public void assignUser(User user) {
        this.user = user;
    }

    private String category;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Builder.Default
    @Column(name = "is_secret", nullable = false)
    private Boolean secret = false;
    private int viewCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintStatus status = ComplaintStatus.WAITING;
    private String answer;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.viewCount = 0;
        if (this.status == null) {
            this.status = ComplaintStatus.WAITING;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 관리자 답변 등록 로직
    public void markAsRead() {      // 최초 1회 (관리자가 상세 내용 조회 시)
        if (this.status == ComplaintStatus.WAITING) {
            this.status = ComplaintStatus.CHECK;
        }
    }

    public void addResponse(String response) {      // 답변 등록 시
        this.answer = response;
        this.status = ComplaintStatus.PROCESSING;
    }

    public void completeComplaint() {       // 최종 해결 완료 후 관리자 클릭
        this.status = ComplaintStatus.COMPLETED;
    }

    // 파일 업로드 연관관계 설정
    @Builder.Default // 빌더 사용 시 초기화된 리스트가 무시되지 않도록 설정
    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ComplaintAttachment> attachments = new ArrayList<>();

    public boolean isSecret() {
        return Boolean.TRUE.equals(this.secret);
    }
}