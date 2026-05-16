package com.hometalk.onepass.community.enums;

public enum BoardType {
    LIST("리스트형"),
    CARD("카드형"),
    GALLERY("갤러리형");

    private final String type;

    BoardType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
