package com.hometalk.onepass.community.dto;

import com.hometalk.onepass.community.enums.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReportSummaryDTO {
    private Long postId;
    private String postTitle;
    private String boardCode;
    private String categoryCode;
    private Long reportCount;
    private Long firstReportId;
    private ReportStatus status;
}
