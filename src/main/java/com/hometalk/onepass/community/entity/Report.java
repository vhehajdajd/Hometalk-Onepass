package com.hometalk.onepass.community.entity;

import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.common.entity.BaseTimeEntity;
import com.hometalk.onepass.community.enums.ReportReason;
import com.hometalk.onepass.community.enums.ReportStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Report extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    private ReportReason reason;     // 신고 사유

    private String detail;           // 신고 상세 내용

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ReportStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    public Report(ReportReason reason, String detail, User user, Post post) {
        this.reason = reason;
        this.detail = detail;
        this.user = user;
        this.post = post;
        this.status = (status != null) ? status : ReportStatus.PENDING;     // 최초 작성 시 대기 상태
    }

    // 관리자 신고 관리
    public void updateStatus(ReportStatus status) {
        this.status = status;
    }

}
