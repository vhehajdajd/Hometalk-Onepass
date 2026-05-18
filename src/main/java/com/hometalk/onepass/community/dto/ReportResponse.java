package com.hometalk.onepass.community.dto;

import com.hometalk.onepass.community.entity.Report;
import com.hometalk.onepass.community.enums.ReportReason;
import com.hometalk.onepass.community.enums.ReportStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ReportResponse {
    private final Long id;
    private final ReportReason reason;
    private final String detail;
    private final String username;  // 신고자 이름
    private final LocalDateTime createdAt;
    private final ReportStatus status;

    private final Long postId;
    private final String postTitle;

    public ReportResponse(Long id, ReportReason reason, String detail, String username,
                          LocalDateTime createdAt, ReportStatus status,
                          Long postId, String postTitle) {
        this.id = id;
        this.reason = reason;
        this.detail = detail;
        this.username = username;
        this.createdAt = createdAt;
        this.status = status;
        this.postId = postId;
        this.postTitle = postTitle;
    }

    public static ReportResponse from(Report report) {
        String reporterName = report.getUser() != null ? report.getUser().getName() : "알 수 없는 사용자";
        Long postId = report.getPost() != null ? report.getPost().getId() : null;
        String postTitle = report.getPost() != null ? report.getPost().getTitle() : "삭제된 게시글";
        return new ReportResponse(
                report.getId(),
                report.getReason(),
                report.getDetail(),
                reporterName,
                report.getCreatedAt(),
                report.getStatus(),
                postId, postTitle
        );
    }
}
