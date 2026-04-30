package com.hometalk.onepass.community.exception;

public class UnauthorizedAccessException extends PostException {

    public UnauthorizedAccessException(String boardCode) {
        super("해당 작업에 대한 권한이 없습니다.", boardCode);
    }
}
