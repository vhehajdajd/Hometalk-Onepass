package com.hometalk.onepass.community.enums;

import lombok.Getter;

@Getter
public enum PostStatus {
    ACTIVE("활성"),
    HIDDEN("숨김"),
    DRAFT("임시저장"),
    DELETED("삭제");

    private final String description;

    PostStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
