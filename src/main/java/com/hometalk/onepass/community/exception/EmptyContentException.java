package com.hometalk.onepass.community.exception;

public class EmptyContentException extends PostException {

    public EmptyContentException(String message, String boardCode) {
        super(message, boardCode);
    }
}
