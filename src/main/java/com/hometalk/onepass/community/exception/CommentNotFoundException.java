package com.hometalk.onepass.community.exception;

public class CommentNotFoundException extends PostException {
    public CommentNotFoundException(Long commentId) {
        super("해당 댓글을 찾을 수 없습니다. id=" + commentId, null);
    }
}
