package com.hometalk.onepass.inquiry.entity;

    // 시스템 오류/ 개발자와 1:1 문의 담당

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder

public class Inquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String memberId;    // 작성자

    @Column(nullable = false)
    private String title;       // 문의 제목(오류 내용 요약)

    private String category;    // 문의 카테고리(로그인,오류제보, 제안 등)

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;     // 문의 상세 내용

    private String answer;      // 개발자 답변 내용
    private String status;      // 문의 상태(답변, 미답변)

    private LocalDateTime createdAt;    // 언제 문의 했는지

}
