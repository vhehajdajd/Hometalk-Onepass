package com.hometalk.onepass.notice.exception;

public class NoticeNotFoundException extends RuntimeException {
    public NoticeNotFoundException(Long id) {
        super("공지를 찾을 수 없습니다. id: " + id);
    }
}