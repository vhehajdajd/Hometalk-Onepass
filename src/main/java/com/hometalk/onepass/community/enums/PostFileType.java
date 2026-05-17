package com.hometalk.onepass.community.enums;

public enum PostFileType {

    THUMBNAIL("썸네일"),
    IMAGE("본문 이미지"),
    ATTACHMENT("첨부파일");

    private final String description;

    PostFileType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
