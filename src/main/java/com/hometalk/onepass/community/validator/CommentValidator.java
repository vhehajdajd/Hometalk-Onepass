package com.hometalk.onepass.community.validator;

import com.hometalk.onepass.community.entity.Comment;
import com.hometalk.onepass.community.exception.UnauthorizedAccessException;
import org.springframework.stereotype.Component;

@Component
public class CommentValidator {

    public void validateOwner(Comment comment, Long userId) {
        if (userId == null || !comment.getWriter().getId().equals(userId)) {
            throw new UnauthorizedAccessException("권한이 없습니다.");
        }
    }
}
