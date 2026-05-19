package com.hometalk.onepass.community.exception;

public class FileStorageException extends RuntimeException {

    // 메시지만 던질 때
    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
