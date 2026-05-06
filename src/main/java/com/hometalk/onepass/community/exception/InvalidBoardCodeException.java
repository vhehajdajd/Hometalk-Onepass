package com.hometalk.onepass.community.exception;

public class InvalidBoardCodeException extends PostException {

    public InvalidBoardCodeException(String boardCode) {
        super("유효하지 않은 게시판 접근입니다: " + boardCode, "free");
    }
}
