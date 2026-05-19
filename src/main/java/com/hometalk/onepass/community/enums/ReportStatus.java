package com.hometalk.onepass.community.enums;

public enum ReportStatus {
    PENDING("대기", "badge-pending"),
    REVIEWING("검토중", "badge-reviewing"),
    RESOLVED("처리완료", "badge-resolved"),
    REJECTED("반려", "badge-rejected");

    private final String displayName;
    private final String badgeClass; // 상태별 배지 CSS 클래스명

    ReportStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}
