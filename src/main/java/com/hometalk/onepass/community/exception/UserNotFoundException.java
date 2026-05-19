package com.hometalk.onepass.community.exception;

public class UserNotFoundException extends PostException {
    public UserNotFoundException(Long userId, String boardCode) {
        super("존재하지 않는 사용자입니다. (ID: " + userId + ")", boardCode);
    }
}
