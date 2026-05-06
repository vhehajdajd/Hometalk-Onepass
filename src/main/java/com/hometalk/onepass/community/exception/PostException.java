package com.hometalk.onepass.community.exception;

import lombok.Getter;

@Getter
public class PostException extends RuntimeException{
    private final String boardCode;

    public PostException(String message, String boardCode) {
        super(message);
        this.boardCode = boardCode;
    }
}
