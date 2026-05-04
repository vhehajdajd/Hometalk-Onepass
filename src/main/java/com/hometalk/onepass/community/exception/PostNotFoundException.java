package com.hometalk.onepass.community.exception;

public class PostNotFoundException extends PostException {

    public PostNotFoundException(Long postId, String boardCode) {
        super("게시글을 찾을 수 없습니다. (ID: " + postId + ")", boardCode);
    }
}
