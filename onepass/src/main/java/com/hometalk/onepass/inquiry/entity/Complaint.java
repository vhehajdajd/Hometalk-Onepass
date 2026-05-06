package com.hometalk.onepass.inquiry.entity;

    // 입주민 민원 게시판 담당

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String memberId;        // 작성자 ID

    private String category;        // 민원 분류 (주차, 공용시설, 등)

    @Column(nullable = false)
    private String title;           // 민원 게시글 제목

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;         // 민원 상세 내용

    private boolean isSecret;       // 비밀글 여부

    private int viewCount;          // 조회수
    private String status;           // 게시글 상태 (답변, 미답변)
    private String answer;          // 관리소 답변 내용

    private LocalDateTime createdAt;    // 게시글 작성 시간
    private LocalDateTime updatedAt;    // 게시글 수정 시간

    // 생성 시간 기록
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now(); // 처음 생성 시각 기록
        this.updatedAt = LocalDateTime.now(); // 처음엔 생성 시각과 수정 시각이 같음
        this.viewCount = 0;
        if (this.status == null) {
            this.status = "접수완료";
        }
    }

    // 수정 시간 기록
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now(); // 데이터가 수정될 때마다 현재 시간으로 갱신
    }


}
