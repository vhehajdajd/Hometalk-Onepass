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
@Table(name = "kjh_inquiry")
public class Inquiry {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public void setUser(User user) { this.user = user; }

    private String category;
    private String title;
    private String content;
    private String status; // 미답변, 처리중, 완료 등
    private String answer;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = "미답변";
    }

    @Builder.Default
    @OneToMany(mappedBy = "inquiry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InquiryAttachment> attachments = new ArrayList<>();


    public void updateAnswer(String answer) {
        this.answer = answer;
        this.status = "완료"; // 답변이 달리면 자동으로 상태 변경
    }


    public void addAttachment(InquiryAttachment attachment) {
        this.attachments.add(attachment);
        if (attachment.getInquiry() != this) {
            attachment.setInquiry(this);
        }
    }
}